package com.superwall.sdk.store

import com.superwall.sdk.billing.DecomposedProductIds
import com.superwall.sdk.models.customer.mergeEntitlementsPrioritized
import com.superwall.sdk.models.customer.toSet
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
            storage
                .read(LatestRedemptionResponse)
                ?.customerInfo
                ?.entitlements
                ?.filter { it.isActive }
                ?.toSet() ?: emptySet()

    // MARK: - Private Properties
    internal val entitlementsByProduct = ConcurrentHashMap<String, Set<Entitlement>>()

    /**
     * Returns a snapshot of all entitlements by product ID.
     * Used when loading purchases to enrich entitlements with transaction data.
     */
    val entitlementsByProductId: Map<String, Set<Entitlement>>
        get() = entitlementsByProduct.toMap()

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
        get() = _all.toSet() + entitlementsByProduct.values.flatten() + web.toSet()

    /**
     * The active entitlements.
     * Uses [mergeEntitlementsPrioritized] to deduplicate entitlements by ID,
     * keeping the highest priority version of each and merging productIds.
     */
    val active: Set<Entitlement>
        get() = mergeEntitlementsPrioritized((backingActive + _activeDeviceEntitlements + web).toList()).toSet()

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
            entitlementsByProduct.putAll(it)
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
                    val entitlements = value.entitlements.toList().toSet()
                    backingActive.addAll(entitlements.filter { it.isActive })
                    _all.addAll(entitlements)
                    _inactive.removeAll(entitlements)
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

    private fun checkFor(
        toCheck: List<String>,
        isExact: Boolean = true,
    ): Set<Entitlement>? {
        if (toCheck.isEmpty()) return null
        val item = toCheck.first()
        val next = toCheck.drop(1)
        return entitlementsByProduct.entries
            .firstOrNull {
                (
                    if (isExact) {
                        it.key == item
                    } else {
                        it.key.contains(item)
                    }
                ) &&
                    it.value.isNotEmpty()
            }?.value ?: checkFor(next, isExact)
    }

    /**
     * Checks for entitlements belonging to the product.
     * First checks exact matches, then checks containing matches
     * by product ID + baseplan and productId  so user doesn't remain without entitlements
     * if they purchased the product. This ensures users dont lose access for their subscription.
     */
    internal fun byProductId(id: String): Set<Entitlement> {
        val decomposedProductIds = DecomposedProductIds.from(id)
        return checkFor(
            listOf(
                decomposedProductIds.fullId,
                "${decomposedProductIds.subscriptionId}:${decomposedProductIds.basePlanId}:${decomposedProductIds.offerType.id ?: ""}",
                "${decomposedProductIds.subscriptionId}:${decomposedProductIds.basePlanId}",
            ),
        ) ?: checkFor(
            listOf(
                "${decomposedProductIds.subscriptionId}:${decomposedProductIds.basePlanId}:",
                decomposedProductIds.subscriptionId,
            ),
            isExact = false,
        ) ?: emptySet()
    }

    /**
     * Returns a Set of Entitlements belonging to given product IDs.
     *
     * @param ids A Set of Strings representing product IDs
     * @return A Set of Entitlements
     */
    fun byProductIds(ids: Set<String>): Set<Entitlement> = ids.flatMap { byProductId(it) }.toSet()

    /**
     * Updates the entitlements associated with product IDs and persists them to storage.
     */
    internal fun addEntitlementsByProductId(idToEntitlements: Map<String, Set<Entitlement>>) {
        entitlementsByProduct.putAll(
            idToEntitlements
                .mapValues { (_, entitlements) ->
                    entitlements.toSet()
                }.toMap(),
        )
        _all.clear()
        _all.addAll(entitlementsByProduct.values.flatten())
        storage.write(StoredEntitlementsByProductId, entitlementsByProduct)
    }
}
