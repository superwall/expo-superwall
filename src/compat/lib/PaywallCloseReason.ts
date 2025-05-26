// src/compat/lib/PaywallCloseReason.ts

/**
 * The reason why a paywall was closed.
 * This is now re-exported from the main SuperwallExpoModule types.
 */
export type { PaywallCloseReason } from '../../SuperwallExpoModule.types';

// Note: The local PaywallCloseReason enum and its associated fromJson/toJson
// methods (within the namespace) have been removed to align with the main types.
// Consumers will now directly use the type from SuperwallExpoModule.types.
// If fromJson/toJson utilities are still needed, they would need to be
// implemented separately or by the consumer.
