/**
 * Represents the base result of a purchase attempt.
 * This class is intended for use within the compatibility layer's PurchaseController.
 */
export class PurchaseResult {
  /**
   * The type of the purchase result.
   */
  public type: string;
  /**
   * An optional error message if the purchase failed.
   */
  public error?: string;

  /**
   * Constructs a PurchaseResult.
   * @param type The type of the result (e.g., 'cancelled', 'purchased', 'failed').
   * @param error An optional error message.
   */
  constructor(type: string, error?: string) {
    this.type = type;
    this.error = error;
  }

  /**
   * Converts the PurchaseResult instance to a JSON object suitable for sending to the native module.
   * The structure matches what `PurchaseResult.fromJson` on the Swift side expects.
   * @returns A JSON object representing the purchase result.
   */
  toJSON(): { type: string; error?: string } {
    const json: { type: string; error?: string } = {
      type: this.type,
    };
    if (this.error) {
      json.error = this.error;
    }
    return json;
  }
}

/**
 * Represents a purchase that was cancelled by the user.
 */
export class PurchaseResultCancelled extends PurchaseResult {
  /**
   * Constructs a PurchaseResultCancelled instance.
   */
  constructor() {
    super('cancelled');
  }
}

/**
 * Represents a successful purchase.
 */
export class PurchaseResultPurchased extends PurchaseResult {
  /**
   * Constructs a PurchaseResultPurchased instance.
   */
  constructor() {
    super('purchased');
  }
}

/**
 * Represents a successful restoration of purchases.
 * Note: This result type is typically used with `didRestore`, not `didPurchase`.
 */
export class PurchaseResultRestored extends PurchaseResult {
  /**
   * Constructs a PurchaseResultRestored instance.
   */
  constructor() {
    super('restored');
  }
}

/**
 * Represents a purchase that is pending and requires further action (e.g., SCA).
 */
export class PurchaseResultPending extends PurchaseResult {
  /**
   * Constructs a PurchaseResultPending instance.
   */
  constructor() {
    super('pending');
  }
}

/**
 * Represents a purchase that failed.
 */
export class PurchaseResultFailed extends PurchaseResult {
  /**
   * Constructs a PurchaseResultFailed instance.
   * @param error A string describing the error.
   */
  constructor(error: string) {
    super('failed', error);
  }
}
