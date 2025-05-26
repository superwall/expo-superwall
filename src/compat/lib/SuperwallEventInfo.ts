// src/compat/lib/SuperwallEventInfo.ts

/**
 * Represents Superwall event information, including the event itself and its parameters.
 * This is now re-exported from the main SuperwallExpoModule types.
 */
export type {
  SuperwallEventInfo,
  SuperwallEvent,
  TriggerResult, // Also re-exporting related types used within SuperwallEvent
  TransactionProductIdentifier,
  // StoreTransaction is already a main type, but re-export for clarity if used directly by consumers of this module
  StoreTransaction,
  RestoreType,
  PaywallPresentationRequestStatus,
  PaywallPresentationRequestStatusReason,
  // EventType is not a concept in the main SuperwallEvent union, which uses string literals directly.
} from '../../SuperwallExpoModule.types';

// Note: The local classes SuperwallEventInfo, SuperwallEvent, and Enum EventType,
// along with their fromJson methods, have been removed to align with main types.
// Consumers will now directly use the types from SuperwallExpoModule.types.
// If fromJson utilities are still needed for these, they would need to be
// implemented separately or by the consumer.
// Other local imports (Entitlement, SubscriptionStatus, PaywallInfo, etc.) are managed
// in their respective files or will be handled in subsequent steps.

// The SuperwallPlacementInfo and SuperwallPlacement aliases are removed as they were
// aliases to the local SuperwallEventInfo and SuperwallEvent which are now removed.
// If these aliases are still needed, they should alias the main types:
// export type { SuperwallEventInfo as SuperwallPlacementInfo } from '../../SuperwallExpoModule.types';
// export type { SuperwallEvent as SuperwallPlacement } from '../../SuperwallExpoModule.types';
// However, to keep this step focused, I'm just removing them for now.
// They can be added back if explicitly required by the overall task.
