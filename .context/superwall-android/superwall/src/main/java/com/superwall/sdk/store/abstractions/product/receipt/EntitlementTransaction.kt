package com.superwall.sdk.store.abstractions.product.receipt

import java.util.Date

/**
 * Represents the type of a product for entitlement processing.
 */
enum class EntitlementTransactionType {
    /** A consumable in-app purchase that can be purchased multiple times. */
    CONSUMABLE,

    /** A non-consumable in-app purchase (lifetime access). */
    NON_CONSUMABLE,

    /** An auto-renewable subscription. */
    AUTO_RENEWABLE,

    /** A non-renewable subscription. */
    NON_RENEWABLE,
}

/**
 * Interface that defines the transaction properties needed for entitlement processing.
 * This abstraction allows for different transaction sources (Google Play, mock data, etc.)
 * to be processed uniformly by the EntitlementProcessor.
 */
interface EntitlementTransaction {
    /** The product identifier for this transaction. */
    val productId: String

    /** The unique identifier for this transaction. */
    val transactionId: String

    /** The date when the purchase was made. */
    val purchaseDate: Date

    /** The original purchase date (for renewals, this is the first purchase). */
    val originalPurchaseDate: Date

    /** The expiration date of the subscription, or null if non-expiring. */
    val expirationDate: Date?

    /** Whether this transaction has been revoked (e.g., refunded). */
    val isRevoked: Boolean

    /** The type of product this transaction represents. */
    val productType: EntitlementTransactionType

    /** Whether the subscription will auto-renew. */
    val willRenew: Boolean

    /** The date when the subscription was last renewed, or null if not renewed. */
    val renewedAt: Date?

    /** Whether the subscription is in a grace period (payment failed but still active). */
    val isInGracePeriod: Boolean

    /** Whether the subscription is in a billing retry period. */
    val isInBillingRetryPeriod: Boolean

    /** Whether the transaction is currently active. */
    val isActive: Boolean
}
