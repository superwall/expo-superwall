/**
 * @category Types
 * @since 0.0.15
 * Defines the log levels for the SDK.
 */
export type LogLevel = "debug" | "info" | "warn" | "error" | "none"

/**
 * @category Types
 * @since 0.0.15
 * Defines the scopes for logging within the SDK.
 */
export type LogScope = 
  | "localizationManager"
  | "bounceButton"
  | "coreData"
  | "configManager"
  | "identityManager"
  | "debugManager"
  | "debugViewController"
  | "localizationViewController"
  | "gameControllerManager"
  | "device"
  | "network"
  | "paywallEvents"
  | "productsManager"
  | "storeKitManager"
  | "placements"
  | "receipts"
  | "superwallCore"
  | "paywallPresentation"
  | "paywallTransactions"
  | "paywallViewController"
  | "cache"
  | "all"

/**
 * @category Types
 * @since 0.0.15
 * Defines the network environment for Superwall.
 */
export type NetworkEnvironment = "release" | "releaseCandidate" | "developer"

/**
 * @category Types
 * @since 0.0.15
 * Defines the different types of views that can appear behind Apple's payment sheet during a transaction.
 */
export type TransactionBackgroundView = "spinner" | "none"

/**
 * @category Models
 * @since 0.0.15
 * Defines the messaging of the alert presented to the user when restoring a transaction fails.
 */
export interface RestoreFailed {
  title: string
  message: string
  closeButtonTitle: string
}

/**
 * @category Models
 * @since 0.0.15
 * Options for configuring logging behavior.
 */
export interface LoggingOptions {
  level: LogLevel
  scopes: LogScope[]
}

/**
 * @category Models
 * @since 0.0.15
 * Options for configuring the appearance and behavior of paywalls.
 */
export interface PaywallOptions {
  isHapticFeedbackEnabled: boolean
  restoreFailed: RestoreFailed
  shouldShowPurchaseFailureAlert: boolean
  shouldPreload: boolean
  automaticallyDismiss: boolean
  transactionBackgroundView: TransactionBackgroundView
}

/**
 * @category Models
 * @since 0.0.15
 * Options for configuring the Superwall SDK.
 */
export interface SuperwallOptions {
  paywalls: PaywallOptions
  networkEnvironment: NetworkEnvironment
  isExternalDataCollectionEnabled: boolean
  localeIdentifier?: string
  isGameControllerEnabled: boolean
  logging: LoggingOptions
  collectAdServicesAttribution: boolean
  passIdentifiersToPlayStore: boolean
  storeKitVersion?: "STOREKIT1" | "STOREKIT2"
  enableExperimentalDeviceVariables: boolean
  manualPurchaseManagement: boolean
}

/**
 * @category Models
 * @since 0.0.15
 * Default options for the Superwall SDK.
 */
export const DefaultSuperwallOptions: SuperwallOptions = {
  paywalls: {
    isHapticFeedbackEnabled: true,
    restoreFailed: {
      title: "No Subscription Found",
      message: "We couldn't find an active subscription for your account.",
      closeButtonTitle: "Okay",
    },
    shouldShowPurchaseFailureAlert: true,
    shouldPreload: false,
    automaticallyDismiss: true,
    transactionBackgroundView: "spinner",
  },
  networkEnvironment: "release",
  isExternalDataCollectionEnabled: true,
  isGameControllerEnabled: false,
  logging: {
    level: "info",
    scopes: ["all"],
  },
  collectAdServicesAttribution: false,
  passIdentifiersToPlayStore: false,
  enableExperimentalDeviceVariables: false,
  manualPurchaseManagement: false,
}