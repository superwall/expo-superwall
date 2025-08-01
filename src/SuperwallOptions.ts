/**
 * @category Enums
 * @since 0.0.15
 * Defines the log levels for the SDK.
 */
export enum LogLevel {
  Debug = "debug",
  Info = "info",
  Warn = "warn",
  Error = "error",
  None = "none",
}

/**
 * @category Enums
 * @since 0.0.15
 * Defines the scopes for logging within the SDK.
 */
export enum LogScope {
  LocalizationManager = "localizationManager",
  BounceButton = "bounceButton",
  CoreData = "coreData",
  ConfigManager = "configManager",
  IdentityManager = "identityManager",
  DebugManager = "debugManager",
  DebugViewController = "debugViewController",
  LocalizationViewController = "localizationViewController",
  GameControllerManager = "gameControllerManager",
  Device = "device",
  Network = "network",
  PaywallEvents = "paywallEvents",
  ProductsManager = "productsManager",
  StoreKitManager = "storeKitManager",
  Placements = "placements",
  Receipts = "receipts",
  SuperwallCore = "superwallCore",
  PaywallPresentation = "paywallPresentation",
  PaywallTransactions = "paywallTransactions",
  PaywallViewController = "paywallViewController",
  Cache = "cache",
  All = "all",
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
 * @category Enums
 * @since 0.0.15
 * Defines the different types of views that can appear behind Apple's payment sheet during a transaction.
 */
export enum TransactionBackgroundView {
  spinner = "spinner",
  none = "none",
}

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
    transactionBackgroundView: TransactionBackgroundView.spinner,
  },
  networkEnvironment: NetworkEnvironment.Release,
  isExternalDataCollectionEnabled: true,
  isGameControllerEnabled: false,
  logging: {
    level: LogLevel.Info,
    scopes: [LogScope.All],
  },
  collectAdServicesAttribution: false,
  passIdentifiersToPlayStore: false,
  enableExperimentalDeviceVariables: false,
  manualPurchaseManagement: false,
}