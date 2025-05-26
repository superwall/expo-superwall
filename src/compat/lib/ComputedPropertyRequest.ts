// src/compat/lib/ComputedPropertyRequest.ts

/**
 * Represents a request for a computed property.
 * This is now re-exported from the main SuperwallExpoModule types.
 */
export type { ComputedPropertyRequest } from '../../SuperwallExpoModule.types';

// Note: The local ComputedPropertyRequest class, ComputedPropertyRequestType enum,
// and their associated fromJson/toJson utility methods have been removed to align
// with the main types. Consumers will now directly use the ComputedPropertyRequest
// type from SuperwallExpoModule.types. The 'type' property within this interface
// is a string, so a separate ComputedPropertyRequestType enum/type is not
// re-exported from the main types. If fromJson/toJson utilities are still needed,
// they would need to be implemented separately or by the consumer.
