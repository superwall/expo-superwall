// src/compat/lib/Entitlement.ts

/**
 * Represents a user entitlement and its type.
 * These are now re-exported from the main SuperwallExpoModule types.
 */
export type {
  Entitlement,
  EntitlementType,
} from '../../SuperwallExpoModule.types';

// Note: The local Entitlement class and its associated fromJson/toJson/create methods
// have been removed to align with the main types. Consumers will now directly use
// the types from SuperwallExpoModule.types. If fromJson/toJson or helper
// creation utilities are still needed, they would need to be implemented
// separately or by the consumer.
