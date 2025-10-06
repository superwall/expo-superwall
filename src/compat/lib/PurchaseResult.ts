/**
 * @category Models
 * @since 0.0.15
 * Base class for purchase results.
 */
export class PurchaseResult {
  constructor(
    public type: "cancelled" | "purchased" | "pending" | "failed",
    public error?: string,
  ) {}

  toJSON():
    | { type: "cancelled" | "purchased" | "pending" }
    | { type: "failed"; error: string } {
    if (this.type === "failed") {
      return {
        type: this.type,
        error: this.error!,
      }
    }
    return {
      type: this.type as "cancelled" | "purchased" | "pending",
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
