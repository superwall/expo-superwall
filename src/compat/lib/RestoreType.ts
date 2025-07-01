import { StoreTransaction } from "./StoreTransaction"

/**
 * @category Enums
 * @since 0.0.15
 * Enum for RestoreType cases.
 */
export enum RestoreTypeCase {
  viaPurchase = 0,
  viaRestore = 1,
}

/**
 * @category Models
 * @since 0.0.15
 * Represents the type of restoration.
 */
export class RestoreType {
  private constructor(
    public type: RestoreTypeCase,
    public storeTransaction?: StoreTransaction,
  ) {}

  // Static methods to create instances of RestoreType
  static viaPurchase(storeTransaction?: StoreTransaction) {
    return new RestoreType(RestoreTypeCase.viaPurchase, storeTransaction)
  }

  static viaRestore = new RestoreType(RestoreTypeCase.viaRestore)

  // Static factory method to deserialize from JSON
  static fromJson(json: any): RestoreType {
    switch (json.type) {
      case "viaPurchase":
        return RestoreType.viaPurchase(
          json.storeTransaction ? StoreTransaction.fromJson(json.storeTransaction) : undefined,
        )
      case "viaRestore":
        return RestoreType.viaRestore
      default:
        throw new Error("Invalid RestoreType type")
    }
  }
}
