import type { CustomerCenterConfiguration, PaywallInfo } from "./SuperwallExpoModule.types"

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
 * @category Types
 * @since 1.6.0
 * Where a presented paywall is hosted on Android.
 * - `"activity"`: the paywall is shown in its own `SuperwallPaywallActivity` (default).
 * - `"inWindow"`: **Experimental.** the paywall is shown as an overlay in the presenting
 *   activity's own window, so that activity is never covered or stopped by the paywall.
 *
 * Android only.
 */
export type PresentationHost = "activity" | "inWindow"

/**
 * @category Types
 * @since 0.3.0
 * Controls when the SDK enters test mode.
 */
export type TestModeBehavior = "automatic" | "whenEnabledForUser" | "never" | "always"

/**
 * Controls which events Superwall collects, for GDPR/data-collection control.
 * - "all": collect all events (default)
 * - "superwallOnly": only Superwall-internal events
 * - "none": collect nothing
 */
export type EventTrackingBehavior = "all" | "superwallOnly" | "none"

/**
 * @category Types
 * @since 1.6.0
 * Whether a user has granted or denied consent for an advertising purpose.
 */
export type AdConsentStatus = "granted" | "denied"

/**
 * @category Types
 * @since 1.6.0
 * A user's consent for how their data is used for advertising, which Superwall passes on with
 * the conversions it uploads to Google Ads and Meta. A purpose left out is `"granted"`.
 *
 * If ad consent is never set, the SDK uses the consent stored by an IAB TCF consent banner when
 * GDPR applies, and otherwise defaults to granted. Apps with users in the EEA, UK or Switzerland
 * that don't use a TCF banner should set it from their consent flow. Both purposes are reported
 * as denied while `eventTrackingBehavior` is `"none"`. On iOS, personalization is also reported as
 * denied when the user hasn't allowed App Tracking Transparency, unless you set it yourself.
 *
 * Supported on iOS and, since 1.8.0, Android.
 */
export interface AdConsent {
  /** Consent to send user data to ad networks for advertising. */
  adUserData?: AdConsentStatus
  /** Consent for them to use that data for personalized advertising. */
  adPersonalization?: AdConsentStatus
}

/**
 * @category Types
 * @since 1.2.0
 * A local asset that can be registered with Superwall and served to the paywall webview
 * via the `swlocal://<id>` URL scheme.
 *
 * Accepts:
 * - A Metro `require()` result (e.g. `require("./hero.png")`) — resolved through `expo-asset`.
 * - A string URI: `file://`, `content://`, or an absolute path.
 * - An `{ uri: string }` object (matches `Image` source).
 */
export type LocalResource = number | string | { uri: string }

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
  shouldShowWebPurchaseConfirmationAlert: boolean
  onBackPressed?: (paywallInfo: PaywallInfo) => boolean
  /**
   * **Experimental.** Chooses how presented paywalls are hosted. Defaults to `"activity"`.
   * Set it to `"inWindow"` to show paywalls as an overlay in the presenting activity's own
   * window instead of launching `SuperwallPaywallActivity`. All presentation styles, back
   * handling, surveys, trial reminder notifications, permission requests and web checkout work
   * in this mode. If the host activity is destroyed while the paywall is shown, including by a
   * configuration change it doesn't handle itself, the paywall is dismissed. This may change or
   * be removed in a future native SDK release.
   *
   * Android only: ignored on iOS.
   * @since 1.6.0
   */
  presentationHost?: PresentationHost
}

/**
 * @category Models
 * @since 0.0.15
 * Options for configuring the Superwall SDK.
 */
export interface SuperwallOptions {
  paywalls: PaywallOptions
  networkEnvironment: NetworkEnvironment
  /** @deprecated Use {@link eventTrackingBehavior} instead. */
  isExternalDataCollectionEnabled: boolean
  eventTrackingBehavior?: EventTrackingBehavior
  localeIdentifier?: string
  isGameControllerEnabled: boolean
  logging: LoggingOptions
  passIdentifiersToPlayStore: boolean
  storeKitVersion?: "STOREKIT1" | "STOREKIT2"
  enableExperimentalDeviceVariables: boolean
  manualPurchaseManagement: boolean
  /**
   * Observe purchases made outside of Superwall. When true, Superwall will observe
   * StoreKit/Play Store transactions and report them. Defaults to false.
   * @platform iOS and Android
   */
  shouldObservePurchases: boolean
  /**
   * Disables the app transaction check on SDK launch. Defaults to false.
   * @platform iOS only
   */
  shouldBypassAppTransactionCheck: boolean
  /**
   * Number of times the SDK will attempt to get the Superwall configuration after
   * a network failure before it times out. Defaults to 6.
   * @platform iOS only
   */
  maxConfigRetryCount: number
  /**
   * Enable mock review functionality. Defaults to false.
   * @platform Android only
   */
  useMockReviews: boolean
  /**
   * Controls when the SDK enters test mode. Defaults to "automatic".
   * @platform iOS and Android
   */
  testModeBehavior: TestModeBehavior
  /**
   * Configures the Customer Center presented with `presentCustomerCenter()`. Omit to use the
   * native default. Can be overridden for a single presentation.
   * @platform iOS and Android
   */
  customerCenter?: CustomerCenterConfiguration
  /**
   * The user's ad consent, passed on with the conversions Superwall uploads to Google Ads and
   * Meta. Omit to use a TCF consent banner's stored answer, or granted. Setting it, even to
   * the default, overrides the banner. Change it at runtime with `setAdConsent()`.
   * @platform iOS and Android (Android since 1.8.0)
   */
  adConsent?: AdConsent
}

/**
 * @category Types
 * @since 0.2.8
 * Deep partial type for SuperwallOptions, allowing partial configuration at any nesting level.
 */
export interface PartialSuperwallOptions {
  paywalls?: Partial<PaywallOptions> & {
    restoreFailed?: Partial<RestoreFailed>
  }
  networkEnvironment?: NetworkEnvironment
  /** @deprecated Use {@link eventTrackingBehavior} instead. */
  isExternalDataCollectionEnabled?: boolean
  eventTrackingBehavior?: EventTrackingBehavior
  localeIdentifier?: string
  isGameControllerEnabled?: boolean
  logging?: Partial<LoggingOptions>
  passIdentifiersToPlayStore?: boolean
  storeKitVersion?: "STOREKIT1" | "STOREKIT2"
  enableExperimentalDeviceVariables?: boolean
  manualPurchaseManagement?: boolean
  /**
   * Observe purchases made outside of Superwall. When true, Superwall will observe
   * StoreKit/Play Store transactions and report them. Defaults to false.
   * @platform iOS and Android
   */
  shouldObservePurchases?: boolean
  /**
   * Disables the app transaction check on SDK launch. Defaults to false.
   * @platform iOS only
   */
  shouldBypassAppTransactionCheck?: boolean
  /**
   * Number of times the SDK will attempt to get the Superwall configuration after
   * a network failure before it times out. Defaults to 6.
   * @platform iOS only
   */
  maxConfigRetryCount?: number
  /**
   * Enable mock review functionality. Defaults to false.
   * @platform Android only
   */
  useMockReviews?: boolean
  /**
   * Controls when the SDK enters test mode. Defaults to "automatic".
   * @platform iOS and Android
   */
  testModeBehavior?: TestModeBehavior
  /**
   * Configures the Customer Center presented with `presentCustomerCenter()`. Omit to use the
   * native default. Can be overridden for a single presentation.
   * @platform iOS and Android
   */
  customerCenter?: CustomerCenterConfiguration
  /**
   * The user's ad consent, passed on with the conversions Superwall uploads to Google Ads and
   * Meta. Omit to use a TCF consent banner's stored answer, or granted. Setting it, even to
   * the default, overrides the banner. Change it at runtime with `setAdConsent()`.
   * @platform iOS and Android (Android since 1.8.0)
   */
  adConsent?: AdConsent
  /**
   * A mapping of local resource IDs to local assets. The paywall webview can reference
   * these via the `swlocal://<id>` URL scheme.
   *
   * Resolved at configure time. Pass a Metro `require()`, a file URI string, or `{ uri }`.
   *
   * ```ts
   * <SuperwallProvider
   *   apiKeys={...}
   *   options={{
   *     localResources: {
   *       "hero-image": require("./assets/hero.png"),
   *       "logo": { uri: FileSystem.documentDirectory + "logo.png" },
   *     }
   *   }}
   * />
   * ```
   *
   * @platform iOS and Android
   */
  localResources?: Record<string, LocalResource>
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
    shouldShowWebPurchaseConfirmationAlert: false,
  },
  networkEnvironment: "release",
  isExternalDataCollectionEnabled: true,
  isGameControllerEnabled: false,
  logging: {
    level: "info",
    scopes: ["all"],
  },
  passIdentifiersToPlayStore: false,
  enableExperimentalDeviceVariables: false,
  manualPurchaseManagement: false,
  shouldObservePurchases: false,
  shouldBypassAppTransactionCheck: false,
  maxConfigRetryCount: 6,
  useMockReviews: false,
  testModeBehavior: "automatic",
}
