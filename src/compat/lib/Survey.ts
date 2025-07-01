/**
 * @category Enums
 * @since 0.0.15
 * Defines the conditions under which a survey can be shown.
 */
export enum SurveyShowCondition {
  onManualClose = "ON_MANUAL_CLOSE",
  onPurchase = "ON_PURCHASE",
}

// Extending the enum functionality using a namespace
export namespace SurveyShowCondition {
  export function toJson(condition: SurveyShowCondition): string {
    return condition
  }

  export function fromJson(json: string): SurveyShowCondition {
    if (!Object.values(SurveyShowCondition).includes(json as SurveyShowCondition)) {
      throw new Error(`Invalid SurveyShowCondition value: ${json}`)
    }
    return json as SurveyShowCondition
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents an option in a survey.
 */
export class SurveyOption {
  id: string
  title: string

  constructor(id: string, title: string) {
    this.id = id
    this.title = title
  }

  static fromJson(json: any): SurveyOption {
    return new SurveyOption(json.id, json.title)
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a survey.
 */
export class Survey {
  id: string
  assignmentKey: string
  title: string
  message: string
  options: SurveyOption[]
  presentationCondition: SurveyShowCondition
  presentationProbability: number
  includeOtherOption: boolean
  includeCloseOption: boolean

  constructor(
    id: string,
    assignmentKey: string,
    title: string,
    message: string,
    options: SurveyOption[],
    presentationCondition: SurveyShowCondition,
    presentationProbability: number,
    includeOtherOption: boolean,
    includeCloseOption: boolean,
  ) {
    this.id = id
    this.assignmentKey = assignmentKey
    this.title = title
    this.message = message
    this.options = options
    this.presentationCondition = presentationCondition
    this.presentationProbability = presentationProbability
    this.includeOtherOption = includeOtherOption
    this.includeCloseOption = includeCloseOption
  }

  static fromJson(json: any): Survey {
    return new Survey(
      json.id,
      json.assignmentKey,
      json.title,
      json.message,
      json.options.map(SurveyOption.fromJson),
      SurveyShowCondition.fromJson(json.presentationCondition),
      Number(json.presentationProbability),
      json.includeOtherOption,
      json.includeCloseOption,
    )
  }
}
