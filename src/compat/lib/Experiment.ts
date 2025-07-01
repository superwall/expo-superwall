/**
 * @category Models
 * @since 0.0.15
 * Represents an experiment a user is part of.
 */
export class Experiment {
  id: string
  groupId: string
  variant: Variant

  constructor(id: string, groupId: string, variant: Variant) {
    this.id = id
    this.groupId = groupId
    this.variant = variant
  }

  static fromJson(json: any): Experiment {
    return new Experiment(json.id, json.groupId, Variant.fromJson(json.variant))
  }

  toJson(): any {
    return {
      id: this.id,
      groupId: this.groupId,
      variant: this.variant.toJson(),
    }
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a variant in an experiment.
 */
export class Variant {
  id: string
  type: VariantType
  paywallId: string | null

  constructor(id: string, type: VariantType, paywallId: string | null) {
    this.id = id
    this.type = type
    this.paywallId = paywallId
  }

  static fromJson(json: any): Variant {
    return new Variant(
      json.id,
      VariantType[json.type as keyof typeof VariantType],
      json.paywallId ?? null,
    )
  }

  toJson(): any {
    return {
      id: this.id,
      type: this.type,
      paywallId: this.paywallId,
    }
  }
}

/**
 * @category Enums
 * @since 0.0.15
 * The type of variant in an experiment.
 */
export enum VariantType {
  TREATMENT = "TREATMENT",
  HOLDOUT = "HOLDOUT",
}
