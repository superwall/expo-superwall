// src/compat/lib/Survey.ts

/**
 * Represents a survey, its options, and presentation condition.
 * These are now re-exported from the main SuperwallExpoModule types.
 */
export type {
  Survey,
  SurveyOption,
  SurveyShowCondition,
} from '../../SuperwallExpoModule.types';

// Note: The local Survey class, SurveyOption class, SurveyShowCondition enum,
// and their associated fromJson/toJson methods have been removed to align
// with the main types. Consumers will now directly use the types from
// SuperwallExpoModule.types. If fromJson/toJson utilities are still needed,
// they would need to be implemented separately or by the consumer.
