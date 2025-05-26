// src/compat/lib/TriggerResult.ts

/**
 * Represents the result of a trigger evaluation.
 * This is now re-exported from the main SuperwallExpoModule types.
 */
export type {
  TriggerResult,
  Experiment, // Re-exporting Experiment as it's part of the TriggerResult type
} from '../../SuperwallExpoModule.types';

// Note: The local `Experiment` import from './Experiment' is still implicitly
// handled by the re-export above if the intention is to use the main Experiment type.
// The local `TriggerResult` class, `TriggerResultType` enum, and its `fromJson` method
// have been removed to align with main types.
// Consumers will now directly use the `TriggerResult` type from SuperwallExpoModule.types.
// If a `fromJson` utility is still needed for TriggerResult, it would need to be
// implemented separately or by the consumer.
