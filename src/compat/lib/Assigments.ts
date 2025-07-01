import { Variant } from "./Experiment"

/**
 * @category Models
 * @since 0.0.15
 * Represents an assignment to an experiment variant.
 */
export class Assignment {
  experimentId: string
  variant: Variant
  isSentToServer: boolean

  constructor(experimentId: string, variant: Variant, isSentToServer: boolean) {
    this.experimentId = experimentId
    this.variant = variant
    this.isSentToServer = isSentToServer
  }

  static fromJson(json: any): Assignment {
    return new Assignment(
      json.experimentId,
      Variant.fromJson(json.variant),
      json.isSentToServer ?? false,
    )
  }
}

/**
 * @category Types
 * @since 0.0.15
 * Represents a confirmed assignment, which is an alias for Assignment.
 */
export type ConfirmedAssignment = Assignment
