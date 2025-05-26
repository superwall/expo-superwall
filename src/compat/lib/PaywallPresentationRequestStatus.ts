// src/compat/lib/PaywallPresentationRequestStatus.ts

/**
 * Represents the status of a paywall presentation request, its reason, and type.
 * These are now re-exported from the main SuperwallExpoModule types.
 */
export type {
  PaywallPresentationRequestStatus,
  PaywallPresentationRequestStatusReason,
  PaywallPresentationRequestStatusType,
  // Re-exporting Experiment as it's part of PaywallPresentationRequestStatusReason
  Experiment,
} from '../../SuperwallExpoModule.types';

// Note: The local Experiment import from './Experiment' is implicitly
// handled by the re-export above if the intention is to use the main Experiment type.
// The local classes and enums related to paywall presentation request status,
// along with their fromJson methods, have been removed to align with main types.
// Consumers will now directly use these types from SuperwallExpoModule.types.
// If fromJson utilities are still needed, they would need to be
// implemented separately or by the consumer.
