// src/compat/lib/Product.ts

/**
 * Represents a product available for purchase on a paywall and its associated entitlements.
 * These are now re-exported from the main SuperwallExpoModule types.
 */
export type {
  Product,
  Entitlement, // Re-exporting Entitlement as it's part of the Product type
} from '../../SuperwallExpoModule.types';

// Note: The local Product class and its fromJson method have been removed
// to align with the main types. The import for the local Entitlement
// class is also no longer needed as we re-export the main Entitlement type.
// Consumers will now directly use the types from SuperwallExpoModule.types.
// If fromJson/toJson utilities are still needed, they would need to be
// implemented separately or by the consumer.
