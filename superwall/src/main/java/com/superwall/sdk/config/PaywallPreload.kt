package com.superwall.sdk.config

import android.content.Context
import com.superwall.sdk.dependencies.RequestFactory
import com.superwall.sdk.dependencies.RuleAttributesFactory
import com.superwall.sdk.misc.IOScope
import com.superwall.sdk.misc.launchWithTracking
import com.superwall.sdk.models.config.Config
import com.superwall.sdk.models.paywall.CacheKey
import com.superwall.sdk.models.paywall.PaywallIdentifier
import com.superwall.sdk.models.triggers.Trigger
import com.superwall.sdk.paywall.manager.PaywallManager
import com.superwall.sdk.paywall.presentation.rule_logic.javascript.RuleEvaluator
import com.superwall.sdk.paywall.request.ResponseIdentifiers
import com.superwall.sdk.paywall.view.webview.webViewExists
import com.superwall.sdk.storage.LocalStorage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class PaywallPreload(
    val factory: Factory,
    val scope: IOScope,
    val storage: LocalStorage,
    val assignments: Assignments,
    val paywallManager: PaywallManager,
) {
    interface Factory :
        RequestFactory,
        RuleAttributesFactory,
        RuleEvaluator.Factory

    private var currentPreloadingTask: Job? = null

    suspend fun preloadAllPaywalls(
        config: Config,
        context: Context,
    ) {
        if (currentPreloadingTask != null) {
            return
        }

        currentPreloadingTask =
            scope.launchWithTracking {
                val expressionEvaluator = factory.provideRuleEvaluator(context)
                val triggers =
                    ConfigLogic.filterTriggers(
                        config.triggers,
                        preloadingDisabled = config.preloadingDisabled,
                    )
                val confirmedAssignments = storage.getConfirmedAssignments()
                val paywallIds =
                    ConfigLogic.getAllActiveTreatmentPaywallIds(
                        triggers = triggers,
                        confirmedAssignments = confirmedAssignments,
                        unconfirmedAssignments = assignments.unconfirmedAssignments,
                        expressionEvaluator = expressionEvaluator,
                    )
                preloadPaywalls(paywallIdentifiers = paywallIds)

                currentPreloadingTask = null
            }
    }

    // Preloads paywalls referenced by the provided triggers.
    suspend fun preloadPaywallsByNames(
        config: Config,
        eventNames: Set<String>,
    ) {
        val triggersToPreload = config.triggers.filter { eventNames.contains(it.eventName) }
        val triggerPaywallIdentifiers =
            getTreatmentPaywallIds(
                config,
                triggersToPreload.toSet(),
            )
        preloadPaywalls(triggerPaywallIdentifiers)
    }

    // Preloads paywalls referenced by triggers.
    private suspend fun preloadPaywalls(paywallIdentifiers: Set<String>) {
        val webviewExists = webViewExists()

        if (webviewExists) {
            scope.launchWithTracking {
                // List to hold all the Deferred objects
                val tasks = mutableListOf<Deferred<Any>>()

                for (identifier in paywallIdentifiers) {
                    val task =
                        async {
                            // Your asynchronous operation
                            val request =
                                factory.makePaywallRequest(
                                    eventData = null,
                                    responseIdentifiers =
                                        ResponseIdentifiers(
                                            paywallId = identifier,
                                            experiment = null,
                                        ),
                                    overrides = null,
                                    isDebuggerLaunched = false,
                                    presentationSourceType = null,
                                )
                            try {
                                paywallManager.getPaywallView(
                                    request = request,
                                    isForPresentation = true,
                                    isPreloading = true,
                                    delegate = null,
                                )
                            } catch (e: Exception) {
                                // Handle exception
                            }
                        }
                    tasks.add(task)
                }
                // Await all tasks
                tasks.awaitAll()
            }
        }
    }

    private fun getTreatmentPaywallIds(
        config: Config,
        triggers: Set<Trigger>,
    ): Set<String> {
        val preloadableTriggers = ConfigLogic.filterTriggers(triggers, config.preloadingDisabled)
        if (preloadableTriggers.isEmpty()) return emptySet()
        val confirmedAssignments = storage.getConfirmedAssignments()
        return ConfigLogic.getActiveTreatmentPaywallIds(
            preloadableTriggers,
            confirmedAssignments,
            assignments.unconfirmedAssignments,
        )
    }

    internal suspend fun removeUnusedPaywallVCsFromCache(
        oldConfig: Config,
        newConfig: Config,
    ) {
        val oldPaywalls = oldConfig.paywalls
        val newPaywalls = newConfig.paywalls

        val presentedPaywallId =
            paywallManager.currentView
                ?.state
                ?.paywall
                ?.identifier
        val oldPaywallCacheIds: Map<PaywallIdentifier, CacheKey> =
            oldPaywalls
                .map { it.identifier to it.cacheKey }
                .toMap()
        val newPaywallCacheIds: Map<PaywallIdentifier, CacheKey> =
            newPaywalls.map { it.identifier to it.cacheKey }.toMap()

        val removedIds: Set<PaywallIdentifier> =
            (oldPaywallCacheIds.keys - newPaywallCacheIds.keys).toSet()

        val changedIds =
            removedIds +
                newPaywalls
                    .filter {
                        val oldCacheKey = oldPaywallCacheIds[it.identifier]
                        val keyChanged = oldCacheKey != newPaywallCacheIds[it.identifier]
                        oldCacheKey != null && keyChanged
                    }.map { it.identifier } - presentedPaywallId

        changedIds.toSet().filterNotNull().forEach {
            paywallManager.removePaywallView(it)
        }
    }
}
