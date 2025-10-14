package com.superwall.sdk.identity

import com.superwall.sdk.Superwall
import com.superwall.sdk.analytics.internal.track
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.awaitFirstValidConfig
import com.superwall.sdk.misc.launchWithTracking
import com.superwall.sdk.misc.sha256MappedToRange
import com.superwall.sdk.network.device.DeviceHelper
import com.superwall.sdk.storage.AliasId
import com.superwall.sdk.storage.AppUserId
import com.superwall.sdk.storage.DidTrackFirstSeen
import com.superwall.sdk.storage.LocalStorage
import com.superwall.sdk.storage.Seed
import com.superwall.sdk.storage.UserAttributes
import com.superwall.sdk.utilities.withErrorTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class IdentityManager(
    private val deviceHelper: DeviceHelper,
    private val storage: LocalStorage,
    private val configManager: ConfigManager,
    private val ioScope: IOScope,
    private val stringToSha: (String) -> String = { it },
) {
    private var _appUserId: String? = storage.read(AppUserId)

    val appUserId: String?
        get() =
            runBlocking(queue) {
                _appUserId
            }

    private var _aliasId: String =
        storage.read(AliasId) ?: IdentityLogic.generateAlias()

    val externalAccountId: String
        get() =
            if (configManager.options.passIdentifiersToPlayStore) {
                userId
            } else {
                stringToSha(userId)
            }

    val aliasId: String
        get() =
            runBlocking(queue) {
                _aliasId
            }

    private var _seed: Int =
        storage.read(Seed) ?: IdentityLogic.generateSeed()

    val seed: Int
        get() =
            runBlocking(queue) {
                _seed
            }

    val userId: String
        get() =
            runBlocking(queue) {
                _appUserId ?: _aliasId
            }

    private var _userAttributes: Map<String, Any> =
        storage.read(UserAttributes) ?: emptyMap()

    val userAttributes: Map<String, Any>
        get() =
            runBlocking(queue) {
                _userAttributes
            }

    val isLoggedIn: Boolean get() = _appUserId != null

    private val identityFlow = MutableStateFlow(false)
    val hasIdentity: Flow<Boolean> get() = identityFlow.asStateFlow().filter { it }

    private val queue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(queue)
    private val identityJobs = CopyOnWriteArrayList<Job>()

    init {
        val extraAttributes = mutableMapOf<String, Any>()

        val aliasId = storage.read(AliasId)
        if (aliasId == null) {
            storage.write(AliasId, _aliasId)
            extraAttributes["aliasId"] = _aliasId
        }

        val seed = storage.read(Seed)
        if (seed == null) {
            storage.write(Seed, _seed)
            extraAttributes["seed"] = _seed
        }

        if (extraAttributes.isNotEmpty()) {
            mergeUserAttributes(
                newUserAttributes = extraAttributes,
                shouldTrackMerge = false,
            )
        }
    }

    fun configure() {
        ioScope.launchWithTracking {
            val neverCalledStaticConfig = storage.neverCalledStaticConfig
            val isFirstAppOpen =
                !(storage.read(DidTrackFirstSeen) ?: false)

            if (IdentityLogic.shouldGetAssignments(
                    isLoggedIn,
                    neverCalledStaticConfig,
                    isFirstAppOpen,
                )
            ) {
                configManager.getAssignments()
            }
            didSetIdentity()
        }
    }

    fun identify(
        userId: String,
        options: IdentityOptions? = null,
    ) {
        scope.launch {
            withErrorTracking {
                IdentityLogic.sanitize(userId)?.let { sanitizedUserId ->
                    if (_appUserId == sanitizedUserId || sanitizedUserId == "") {
                        if (sanitizedUserId == "") {
                            Logger.debug(
                                logLevel = LogLevel.error,
                                scope = LogScope.identityManager,
                                message = "The provided userId was empty.",
                            )
                        }
                        return@withErrorTracking
                    }

                    identityFlow.emit(false)

                    val oldUserId = _appUserId
                    if (oldUserId != null && sanitizedUserId != oldUserId) {
                        Superwall.instance.reset(duringIdentify = true)
                    }

                    _appUserId = sanitizedUserId

                    // If we haven't gotten config yet, we need
                    // to leave this open to grab the appUserId for headers
                    identityJobs +=
                        ioScope.launch {
                            val config = configManager.configState.awaitFirstValidConfig()

                            if (config?.featureFlags?.enableUserIdSeed == true) {
                                sanitizedUserId.sha256MappedToRange()?.let { seed ->
                                    _seed = seed
                                    saveIds()
                                }
                            }
                        }

                    saveIds()

                    ioScope.launch {
                        val trackableEvent = InternalSuperwallEvent.IdentityAlias()
                        Superwall.instance.track(trackableEvent)
                    }

                    configManager.checkForWebEntitlements()

                    if (options?.restorePaywallAssignments == true) {
                        identityJobs +=
                            ioScope.launch {
                                configManager.getAssignments()
                                didSetIdentity()
                            }
                    } else {
                        ioScope.launch {
                            configManager.getAssignments()
                        }
                        didSetIdentity()
                    }
                }
            }
        }
    }

    private fun didSetIdentity() {
        scope.launch {
            identityJobs.forEach { it.join() }
            identityFlow.emit(true)
        }
    }

    /**
     * Saves the `aliasId`, `seed` and `appUserId` to storage and user attributes.
     */
    private fun saveIds() {
        withErrorTracking {
            // This is not wrapped in a scope/mutex because is
            // called from the didSet of vars, who are already
            // being set within the queue.
            _appUserId?.let {
                storage.write(AppUserId, it)
            } ?: kotlin.run { storage.delete(AppUserId) }
            storage.write(AliasId, _aliasId)
            storage.write(Seed, _seed)

            val newUserAttributes =
                mutableMapOf(
                    "aliasId" to _aliasId,
                    "seed" to _seed,
                )
            _appUserId?.let { newUserAttributes["appUserId"] = it }

            _mergeUserAttributes(
                newUserAttributes = newUserAttributes,
            )
        }
    }

    fun reset(duringIdentify: Boolean) {
        ioScope.launch {
            identityFlow.emit(false)
        }

        if (duringIdentify) {
            _reset()
        } else {
            _reset()
            didSetIdentity()
        }
    }

    @Suppress("ktlint:standard:function-naming")
    private fun _reset() {
        _appUserId = null
        _aliasId = IdentityLogic.generateAlias()
        _seed = IdentityLogic.generateSeed()
        _userAttributes = emptyMap()
        saveIds()
    }

    fun mergeUserAttributes(
        newUserAttributes: Map<String, Any?>,
        shouldTrackMerge: Boolean = true,
    ) {
        scope.launch {
            _mergeUserAttributes(
                newUserAttributes = newUserAttributes,
                shouldTrackMerge = shouldTrackMerge,
            )
        }
    }

    @Suppress("ktlint:standard:function-naming")
    private fun _mergeUserAttributes(
        newUserAttributes: Map<String, Any?>,
        shouldTrackMerge: Boolean = true,
    ) {
        withErrorTracking {
            val mergedAttributes =
                IdentityLogic.mergeAttributes(
                    newAttributes = newUserAttributes,
                    oldAttributes = _userAttributes,
                    appInstalledAtString = deviceHelper.appInstalledAtString,
                )

            if (shouldTrackMerge) {
                ioScope.launch {
                    val trackableEvent =
                        InternalSuperwallEvent.Attributes(
                            deviceHelper.appInstalledAtString,
                            HashMap(mergedAttributes),
                        )
                    Superwall.instance.track(trackableEvent)
                }
            }
            storage.write(UserAttributes, mergedAttributes)
            _userAttributes = mergedAttributes
        }
    }
}
