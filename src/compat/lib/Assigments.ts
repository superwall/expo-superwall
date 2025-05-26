import { Variant } from "./Experiment"

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

export type ConfirmedAssignment = Assignment
