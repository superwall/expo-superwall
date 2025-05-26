// src/compat/lib/Experiment.ts

/**
 * Represents a Superwall experiment, its variant, and variant type.
 * These are now re-exported from the main SuperwallExpoModule types.
 */
export type {
  Experiment,
  Variant,
  VariantType,
} from '../../SuperwallExpoModule.types';

// Note: The local Experiment class, Variant class, VariantType enum,
// and their associated fromJson/toJson methods have been removed to align
// with the main types. Consumers will now directly use the types from
// SuperwallExpoModule.types. If fromJson/toJson utilities are still needed,
// they would need to be implemented separately or by the consumer.
