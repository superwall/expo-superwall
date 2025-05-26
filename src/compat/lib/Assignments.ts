import type { Variant } from '../../SuperwallExpoModule.types'; // Explicitly import main Variant

/**
 * Represents an experiment assignment for a user.
 * This includes the experiment ID, the variant the user was assigned to,
 * and whether this assignment has been reported to the server.
 */
export class Assignment {
  /**
   * The identifier of the experiment.
   */
  experimentId: string; // Changed from String to string
  /**
   * The variant of the experiment that the user was assigned to.
   */
  variant: Variant; // Uses MainVariant
  /**
   * Indicates whether this assignment has been sent to the server.
   */
  isSentToServer: boolean; // Changed from Boolean to boolean

  /**
   * Constructs an instance of Assignment.
   * @param experimentId The identifier of the experiment.
   * @param variant The variant of the experiment.
   * @param isSentToServer Whether the assignment has been sent to the server.
   */
  constructor(experimentId: string, variant: Variant, isSentToServer: boolean) {
    this.experimentId = experimentId;
    this.variant = variant;
    this.isSentToServer = isSentToServer;
  }

  /**
   * Creates an Assignment instance from a JSON object.
   * @param json The JSON object to parse. Expected to have `experimentId`, `variant` (conforming to Variant.fromJson structure), and optionally `isSentToServer`.
   * @returns A new Assignment instance.
   */
  static fromJson(json: any): Assignment {
    // The Variant.fromJson was removed when Experiment.ts was updated to re-export main types.
    // Assuming json.variant is already compatible with the MainVariant interface.
    // If json.variant still needs parsing, that logic would need to be here or
    // Variant should be a class with fromJson in the compat layer.
    // For now, casting directly, assuming structure matches.
    const variant: Variant = json.variant as Variant;

    return new Assignment(
      json.experimentId,
      variant, // Use the main Variant type directly
      json.isSentToServer ?? false
    );
  }
}

/**
 * Represents a confirmed assignment, which is structurally the same as an Assignment.
 */
export type ConfirmedAssignment = Assignment;
