import { LogLevel } from "./LogLevel"
import { LogScope } from "./LogScope"
import { PaywallOptions } from "./PaywallOptions"

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
    return {
      level: this.level,
      scopes: this.scopes,
    }
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
  collectAdServicesAttribution = false
  passIdentifiersToPlayStore = false
  storeKitVersion?: "STOREKIT1" | "STOREKIT2"
  enableExperimentalDeviceVariables = false

  constructor(init?: Partial<SuperwallOptions>) {
    if (init) {
      if (init.paywalls) {
        this.paywalls = new PaywallOptions(init.paywalls) // Pass init.paywalls to PaywallOptions constructor
      }
      // Assign other properties, ensuring paywalls is handled correctly
      const { paywalls, ...restInit } = init
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
    return {
      paywalls: this.paywalls.toJson(),
      networkEnvironment: this.networkEnvironment,
      isExternalDataCollectionEnabled: this.isExternalDataCollectionEnabled,
      localeIdentifier: this.localeIdentifier,
      isGameControllerEnabled: this.isGameControllerEnabled,
      logging: this.logging.toJson(),
      collectAdServicesAttribution: this.collectAdServicesAttribution,
      passIdentifiersToPlayStore: this.passIdentifiersToPlayStore,
      storeKitVersion: this.storeKitVersion,
      enableExperimentalDeviceVariables: this.enableExperimentalDeviceVariables,
    }
  }
}
