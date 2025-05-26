// src/compat/lib/StoreTransaction.ts

/**
 * Represents a store transaction.
 * This is now re-exported from the main SuperwallExpoModule types.
 * Dates are expected to be in ISO 8601 string format.
 */
export type { StoreTransaction } from '../../SuperwallExpoModule.types';

// Note: The local StoreTransaction class and its fromJson method have been removed
// to align with the main types. The main StoreTransaction type expects date
// properties to be strings (ISO 8601 format), not Date objects.
// Consumers will now directly use the type from SuperwallExpoModule.types.
// If fromJson utilities or Date object conversion are still needed, they would
// need to be implemented separately or by the consumer.
