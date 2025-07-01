/**
 * @category Models
 * @since 0.0.15
 * Base class for purchase results.
 */
export class PurchaseResult {
  constructor(
    public type: string,
    public error?: string,
  ) {}

  toJSON() {
    return {
      type: this.type,
      ...(this.error && { error: this.error }), // Conditionally add error field if present
    }
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a cancelled purchase.
 */
export class PurchaseResultCancelled extends PurchaseResult {
  constructor() {
    super("cancelled")
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a successful purchase.
 */
export class PurchaseResultPurchased extends PurchaseResult {
  constructor() {
    super("purchased")
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a successful restoration of purchases.
 */
export class PurchaseResultRestored extends PurchaseResult {
  constructor() {
    super("restored")
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a pending purchase.
 */
export class PurchaseResultPending extends PurchaseResult {
  constructor() {
    super("pending")
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Represents a failed purchase.
 */
export class PurchaseResultFailed extends PurchaseResult {
  constructor(error: string) {
    super("failed", error)
  }
}
