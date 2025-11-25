import { LogLevel } from "./LogLevel"
import { LogScope } from "./LogScope"
import { PaywallOptions } from "./PaywallOptions"

/**
 * Helper function to remove undefined values from an object.
 * This is necessary for Android compatibility as the Expo bridge
 * cannot convert undefined to Kotlin types.
 */
function filterUndefined<T extends Record<string, any>>(obj: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(obj).filter(([_, value]) => value !== undefined),
  ) as Partial<T>
}

/**
 * @category Enums
 * @since 0.0.15
 * Defines the network environment for Superwall.
 */
export enum NetworkEnvironment {
  Release = "release",
  ReleaseCandidate = "releaseCandidate",
  Developer = "developer",
}

/**
 * @category Models
 * @since 0.0.15
 * Options for configuring logging behavior.
 */
export class LoggingOptions {
  level: LogLevel = LogLevel.Info
  scopes: LogScope[] = [LogScope.All]

  toJson(): object {
    return filterUndefined({
      level: this.level,
      scopes: this.scopes,
    })
  }
}

/**
 * @category Models
 * @since 0.0.15
 * Options for configuring the Superwall SDK.
 */
export class SuperwallOptions {
  paywalls: PaywallOptions = new PaywallOptions()
  networkEnvironment: NetworkEnvironment = NetworkEnvironment.Release
  isExternalDataCollectionEnabled = true
  localeIdentifier?: string
  isGameControllerEnabled = false
  logging: LoggingOptions = new LoggingOptions()
  passIdentifiersToPlayStore = false
  storeKitVersion?: "STOREKIT1" | "STOREKIT2"
  enableExperimentalDeviceVariables = false
  /**
   * Observe purchases made outside of Superwall. When true, Superwall will observe
   * StoreKit/Play Store transactions and report them. Defaults to false.
   * @platform iOS and Android
   */
  shouldObservePurchases = false
  /**
   * Disables the app transaction check on SDK launch. Defaults to false.
   * @platform iOS only
   */
  shouldBypassAppTransactionCheck = false
  /**
   * Number of times the SDK will attempt to get the Superwall configuration after
   * a network failure before it times out. Defaults to 6.
   * @platform iOS only
   */
  maxConfigRetryCount = 6
  /**
   * Enable mock review functionality. Defaults to false.
   * @platform Android only
   */
  useMockReviews = false

  constructor(init?: Partial<SuperwallOptions>) {
    if (init) {
      if (init.paywalls) {
        this.paywalls = new PaywallOptions(init.paywalls) // Pass init.paywalls to PaywallOptions constructor
      }
      if (init.logging) {
        // Ensure logging is always a LoggingOptions instance
        this.logging =
          init.logging instanceof LoggingOptions
            ? init.logging
            : Object.assign(new LoggingOptions(), init.logging)
      }
      // Assign other properties, excluding nested objects
      // biome-ignore lint/correctness/noUnusedVariables: Extracted to exclude from spread
      const { paywalls, logging, ...restInit } = init
      Object.assign(this, restInit)
      if (paywalls && !init.paywalls) {
        // if paywalls was in init but not used for constructor
        this.paywalls = new PaywallOptions(paywalls)
      }
    }
  }

  // You can add methods to this class if needed
  toJson(): object {
    // Method to serialize class instance to a plain object, useful when passing to native code
    // Filter out undefined values to prevent Android Expo bridge conversion errors
    return filterUndefined({
      paywalls: this.paywalls.toJson(),
      networkEnvironment: this.networkEnvironment,
      isExternalDataCollectionEnabled: this.isExternalDataCollectionEnabled,
      localeIdentifier: this.localeIdentifier,
      isGameControllerEnabled: this.isGameControllerEnabled,
      logging: this.logging.toJson(),
      passIdentifiersToPlayStore: this.passIdentifiersToPlayStore,
      storeKitVersion: this.storeKitVersion,
      enableExperimentalDeviceVariables: this.enableExperimentalDeviceVariables,
      shouldObservePurchases: this.shouldObservePurchases,
      shouldBypassAppTransactionCheck: this.shouldBypassAppTransactionCheck,
      maxConfigRetryCount: this.maxConfigRetryCount,
      useMockReviews: this.useMockReviews,
    })
  }
}
