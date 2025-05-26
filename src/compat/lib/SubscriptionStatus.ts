// src/compat/lib/SubscriptionStatus.ts

/**
 * Represents the subscription status of the user.
 * This is now re-exported from the main SuperwallExpoModule types.
 */
export type { SubscriptionStatus } from '../../SuperwallExpoModule.types';

// Note: The local `Entitlement` import is still present but will be addressed
// in a subsequent step. The local `SubscriptionStatus` type and namespace,
// including its `fromJson` method, have been removed to align with main types.
// Consumers will now directly use the `SubscriptionStatus` type from SuperwallExpoModule.types.
// If a `fromJson` utility is still needed for SubscriptionStatus, it would need to be
// implemented separately or by the consumer.
