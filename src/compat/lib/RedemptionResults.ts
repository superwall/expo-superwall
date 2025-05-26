// src/compat/lib/RedemptionResults.ts

/**
 * Represents the result of a promotional code redemption attempt.
 * This is now re-exported from the main SuperwallExpoModule types.
 */
export type {
  RedemptionResult,
  RedemptionInfo,
  RedemptionErrorInfo,
  RedemptionExpiredCodeInfo,
  RedemptionOwnership,
  RedemptionStoreIdentifiers,
  RedemptionPurchaserInfo,
  RedemptionPaywallInfo,
} from '../../SuperwallExpoModule.types';

// Note: The local `Entitlement` import and the `RedemptionResults` class with its `fromJson`
// method have been removed as per the task to align with main types.
// Consumers of this compat layer will now directly use the types from SuperwallExpoModule.types.
// If a `fromJson` utility is still needed for RedemptionResult, it would need to be
// implemented separately, potentially in a new utility file or by the consumer,
// as the main types do not include such methods.
