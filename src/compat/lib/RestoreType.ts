// src/compat/lib/RestoreType.ts

/**
 * Represents the type of restoration and the associated store transaction.
 * These are now re-exported from the main SuperwallExpoModule types.
 */
export type {
  RestoreType,
  StoreTransaction, // Re-exporting StoreTransaction as it's part of the RestoreType type
} from '../../SuperwallExpoModule.types';

// Note: The local StoreTransaction import from './StoreTransaction' is implicitly
// handled by the re-export above if the intention is to use the main StoreTransaction type.
// The local RestoreType class, RestoreTypeCase enum, and its fromJson method
// have been removed to align with main types.
// Consumers will now directly use the RestoreType type from SuperwallExpoModule.types.
// If a fromJson utility is still needed for RestoreType, it would need to be
// implemented separately or by the consumer.
