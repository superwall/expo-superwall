// src/compat/lib/FeatureGatingBehavior.ts

/**
 * Defines the behavior for feature gating.
 * This is now re-exported from the main SuperwallExpoModule types.
 */
export type { FeatureGatingBehavior } from '../../SuperwallExpoModule.types';

// Note: The local FeatureGatingBehavior enum and its associated fromJson/toJson
// helper functions have been removed to align with the main types.
// Consumers will now directly use the type from SuperwallExpoModule.types.
// If fromJson/toJson utilities are still needed, they would need to be
// implemented separately or by the consumer.
