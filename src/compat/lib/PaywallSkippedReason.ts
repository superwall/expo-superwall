import { Experiment } from "./Experiment"

/**
 * @category Models
 * @since 0.0.15
 * Abstract class representing the reason why a paywall was skipped.
 */
export abstract class PaywallSkippedReason extends Error {
  constructor(message?: string) {
    super(message)
    this.name = new.target.name
  }

  static fromJson(json: any): PaywallSkippedReason {
    switch (json.type) {
      case "Holdout": {
        const experiment = Experiment.fromJson(json.experiment)
        return new PaywallSkippedReasonHoldout(experiment)
      }
      case "NoAudienceMatch":
        return new PaywallSkippedReasonNoAudienceMatch()
      case "PlacementNotFound":
        return new PaywallSkippedReasonPlacementNotFound()
      case "UserIsSubscribed":
        return new PaywallSkippedReasonUserIsSubscribed()
      default:
        throw new Error("Unknown PaywallSkippedReason type")
    }
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Paywall skipped due to user being in a holdout group.
 */
export class PaywallSkippedReasonHoldout extends PaywallSkippedReason {
  experiment: Experiment

  constructor(experiment: Experiment) {
    super("Holdout")
    this.experiment = experiment
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Paywall skipped due to no audience match.
 */
export class PaywallSkippedReasonNoAudienceMatch extends PaywallSkippedReason {
  constructor() {
    super("NoAudienceMatch")
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Paywall skipped due to placement not being found.
 */
export class PaywallSkippedReasonPlacementNotFound extends PaywallSkippedReason {
  constructor() {
    super("PlacementNotFound")
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Paywall skipped due to user already being subscribed.
 */
export class PaywallSkippedReasonUserIsSubscribed extends PaywallSkippedReason {
  constructor() {
    super("UserIsSubscribed")
  }
}
