/**
 * @category Models
 * @since 0.0.15
 * Abstract class representing the result of a purchase restoration.
 */
export abstract class RestorationResult {
  // biome-ignore lint/complexity/noBannedTypes: <explanation>
  abstract toJson(): Object

  static restored() {
    return new Restored()
  }

  static failed(error?: Error) {
    return new Failed(error)
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a successful restoration of purchases.
 */
export class Restored extends RestorationResult {
  toJson() {
    return { result: "restored" }
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a failed restoration of purchases.
 */
export class Failed extends RestorationResult {
  constructor(public error?: Error) {
    super()
  }

  toJson() {
    return {
      result: "failed",
      errorMessage: this.error ? this.error.message : null,
    }
  }
}
