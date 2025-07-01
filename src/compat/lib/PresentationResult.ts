import { Experiment } from "./Experiment"

/**
 * @category Models
 * @since 0.0.15
 * Abstract class representing the result of a presentation request.
 */
// biome-ignore lint/complexity/noStaticOnlyClass: <explanation>
export abstract class PresentationResult {
  static fromJson(json: any): PresentationResult {
    let experiment: Experiment | undefined
    if (json.experiment) {
      experiment = Experiment.fromJson(json.experiment)
    }

    switch (json.type) {
      case "Holdout":
        if (!experiment) throw new Error("Holdout requires an experiment")
        return new PresentationResultHoldout(experiment)
      case "Paywall":
        if (!experiment) throw new Error("Paywall requires an experiment")
        return new PresentationResultPaywall(experiment)
      case "NoAudienceMatch":
        return new PresentationResultNoAudienceMatch()
      case "PlacementNotFound":
        return new PresentationResultPlacementNotFound()
      case "UserIsSubscribed":
        return new PresentationResultUserIsSubscribed()
      case "PaywallNotAvailable":
        return new PresentationResultPaywallNotAvailable()
      default:
        throw new Error("Unknown PresentationResult type")
    }
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Presentation result: Placement not found.
 */
export class PresentationResultPlacementNotFound extends PresentationResult {}

/**
 * @category Models
 * @since 0.0.15
 * Presentation result: No audience match.
 */
export class PresentationResultNoAudienceMatch extends PresentationResult {}

/**
 * @category Models
 * @since 0.0.15
 * Presentation result: User is already subscribed.
 */
export class PresentationResultUserIsSubscribed extends PresentationResult {}

/**
 * @category Models
 * @since 0.0.15
 * Presentation result: Paywall not available.
 */
export class PresentationResultPaywallNotAvailable extends PresentationResult {}

/**
 * @category Models
 * @since 0.0.15
 * Presentation result: User is in a holdout group.
 */
export class PresentationResultHoldout extends PresentationResult {
  experiment: Experiment

  constructor(experiment: Experiment) {
    super()
    this.experiment = experiment
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Presentation result: Paywall was presented.
 */
export class PresentationResultPaywall extends PresentationResult {
  experiment: Experiment

  constructor(experiment: Experiment) {
    super()
    this.experiment = experiment
  }
}
