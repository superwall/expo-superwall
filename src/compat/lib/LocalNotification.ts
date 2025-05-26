// src/compat/lib/LocalNotification.ts

/**
 * Represents a local notification and its type.
 * These are now re-exported from the main SuperwallExpoModule types.
 */
export type {
  LocalNotification,
  LocalNotificationType,
} from '../../SuperwallExpoModule.types';

// Note: The local LocalNotification class, LocalNotificationType enum,
// and their associated fromJson/toJson utility methods (within LocalNotificationTypeUtils)
// have been removed to align with the main types. Consumers will now directly use
// the types from SuperwallExpoModule.types. If fromJson/toJson utilities
// are still needed, they would need to be implemented separately or by the consumer.
