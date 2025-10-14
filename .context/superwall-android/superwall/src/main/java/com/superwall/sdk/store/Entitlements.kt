package com.superwall.sdk.store

import com.superwall.sdk.billing.DecomposedProductIds
import com.superwall.sdk.models.entitlements.Entitlement
import com.superwall.sdk.models.entitlements.SubscriptionStatus
import com.superwall.sdk.storage.LatestRedemptionResponse
import com.superwall.sdk.storage.Storage
import com.superwall.sdk.storage.StoredEntitlementsByProductId
import com.superwall.sdk.storage.StoredSubscriptionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * A class that handles the Set of Entitlement objects retrieved from
 * the Superwall dashboard.
 */
class Entitlements(
    private val storage: Storage,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    val web: Set<Entitlement>
        get() =
            storage.read(LatestRedemptionResponse)?.entitlements?.toSet() ?: emptySet()

    // MARK: - Private Properties
    private val _entitlementsByProduct = ConcurrentHashMap<String, Set<Entitlement>>()

    private val _status: MutableStateFlow<SubscriptionStatus> =
        MutableStateFlow(SubscriptionStatus.Unknown)

    /**
     * A StateFlow of the entitlement status of the user. Set this using
     * [Superwall.instance.setEntitlementStatus].
     *
     * You can collect this flow to get notified whenever it changes.
     */
    val status: StateFlow<SubscriptionStatus>
        get() = _status.asStateFlow()

    // MARK: - Backing Fields

    /**
     * Internal backing variable that is set only via setSubscriptionStatus
     */
    private var backingActive: MutableSet<Entitlement> = mutableSetOf()

    private val _all = mutableSetOf<Entitlement>()
    private val _activeDeviceEntitlements = mutableSetOf<Entitlement>()
    private val _inactive = _all.subtract(backingActive).toMutableSet()
    // MARK: - Public Properties

    internal var activeDeviceEntitlements: Set<Entitlement>
        get() = _activeDeviceEntitlements
        set(value) {
            _activeDeviceEntitlements.clear()
            _activeDeviceEntitlements.addAll(value)
        }

    /**
     * All entitlements, regardless of whether they're active or not.
     */
    val all: Set<Entitlement>
        get() = _all.toSet() + _entitlementsByProduct.values.flatten() + web.toSet()

    /**
     * The active entitlements.
     */
    val active: Set<Entitlement>
        get() = backingActive + _activeDeviceEntitlements + web

    /**
     * The inactive entitlements.
     */
    val inactive: Set<Entitlement>
        get() = _inactive.toSet() + all.minus(active)

    init {
        storage.read(StoredSubscriptionStatus)?.let {
            setSubscriptionStatus(it)
        }
        storage.read(StoredEntitlementsByProductId)?.let {
            _entitlementsByProduct.putAll(it)
        }

        scope.launch {
            status.collect {
                storage.write(StoredSubscriptionStatus, it)
            }
        }
    }

    /**
     * Sets the entitlement status and updates the corresponding entitlement collections.
     */
    fun setSubscriptionStatus(value: SubscriptionStatus) {
        when (value) {
            is SubscriptionStatus.Active -> {
                if (value.entitlements.isEmpty()) {
                    setSubscriptionStatus(SubscriptionStatus.Inactive)
                } else {
                    backingActive.addAll(value.entitlements)
                    _all.addAll(value.entitlements)
                    _inactive.removeAll(value.entitlements)
                    _status.value = value
                }
            }

            is SubscriptionStatus.Inactive -> {
                _activeDeviceEntitlements.clear()
                backingActive.clear()
                _inactive.clear()
                _status.value = value
            }

            is SubscriptionStatus.Unknown -> {
                backingActive.clear()
                _activeDeviceEntitlements.clear()
                _inactive.clear()
                _status.value = value
            }
        }
    }

    /**
     * Returns a Set of Entitlements belonging to a given productId.
     *
     * @param id A String representing a productId
     * @return A Set of Entitlements
     */
    internal fun byProductId(id: String): Set<Entitlement> {
        val decomposedProductIds = DecomposedProductIds.from(id)
        listOf(
            decomposedProductIds.fullId,
            "${decomposedProductIds.subscriptionId}:${decomposedProductIds.basePlanId}",
            decomposedProductIds.subscriptionId,
        ).forEach { id ->
            _entitlementsByProduct.entries
                .firstOrNull { it.key.contains(id) && it.value.isNotEmpty() }
                .let {
                    if (it != null) {
                        return it.value
                    }
                }
        }
        return emptySet()
    }

    /**
     * Updates the entitlements associated with product IDs and persists them to storage.
     */
    internal fun addEntitlementsByProductId(idToEntitlements: Map<String, Set<Entitlement>>) {
        _entitlementsByProduct.putAll(
            idToEntitlements
                .mapValues { (_, entitlements) ->
                    entitlements.toSet()
                }.toMap(),
        )
        _all.clear()
        _all.addAll(_entitlementsByProduct.values.flatten())
        storage.write(StoredEntitlementsByProductId, _entitlementsByProduct)
    }
}
