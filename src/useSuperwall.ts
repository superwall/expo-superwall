import { createContext, useContext } from "react"
import { create } from "zustand"
import { useShallow } from "zustand/shallow"
import pkg from "../package.json"
import type { PresentationResult } from "./compat/lib/PresentationResult"
import {
  customerCenterConfigurationToJson,
  dismissCustomerCenter,
  type PresentCustomerCenterOptions,
  presentCustomerCenter,
} from "./internal/customerCenter"
import { resolveLocalResources } from "./localResources"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type {
  Assignment,
  ConfigurationStatus,
  CustomerInfo,
  Entitlement,
  EntitlementsInfo,
  IntegrationAttribute,
  IntegrationAttributes,
  InterfaceStyle,
  LogLevel,
  OwnedInAppPurchase,
  PaywallInfo,
  ProductResponse,
  PurchaseResultResponse,
  PurchasingObserverState,
  RestorationResultResponse,
  ShowAlertOptions,
  SubscriptionStatus,
} from "./SuperwallExpoModule.types"
import {
  type AdConsent,
  DefaultSuperwallOptions,
  type EventTrackingBehavior,
  type PartialSuperwallOptions,
} from "./SuperwallOptions"
import { filterUndefined } from "./utils/filterUndefined"
import { productIdentifierForPurchase } from "./utils/productIdentifier"
import { purchasingObserverStateToJson } from "./utils/purchasingObserverState"

const CONFIGURE_WAIT_TIMEOUT_MS = 10_000

/**
 * Blocks a native call until Superwall has finished configuring.
 *
 * Resolves as soon as `isConfigured` flips true, rejects if configuration failed
 * (`configurationError`), and otherwise rejects after CONFIGURE_WAIT_TIMEOUT_MS.
 * The timeout clock (re)starts when configure() begins — so a configure() called
 * late still gets the full window — and the error distinguishes "never called"
 * from "called but too slow". Subscribes to the store rather than polling.
 */
function awaitConfigured(): Promise<void> {
  const { isConfigured, configurationError } = useSuperwallStore.getState()
  if (isConfigured) return Promise.resolve()
  if (configurationError) return Promise.reject(new Error(configurationError))

  return new Promise<void>((resolve, reject) => {
    let timer: ReturnType<typeof setTimeout>

    const settle = (fn: () => void) => {
      clearTimeout(timer)
      unsubscribe()
      fn()
    }

    // Restart the clock when configure starts so a late configure() gets the full
    // window instead of inheriting time already spent waiting before it ran.
    const arm = () => {
      clearTimeout(timer)
      timer = setTimeout(() => {
        const inFlight = useSuperwallStore.getState().isLoading
        settle(() =>
          reject(
            new Error(
              inFlight
                ? `Superwall configure did not complete within ${CONFIGURE_WAIT_TIMEOUT_MS}ms.`
                : `Superwall configure was not called within ${CONFIGURE_WAIT_TIMEOUT_MS}ms. Did you forget to call configure()?`,
            ),
          ),
        )
      }, CONFIGURE_WAIT_TIMEOUT_MS)
    }

    const unsubscribe = useSuperwallStore.subscribe((state, prev) => {
      if (state.isConfigured) {
        settle(resolve)
      } else if (state.configurationError) {
        const error = state.configurationError
        settle(() => reject(new Error(error)))
      } else if (state.isLoading && !prev.isLoading) {
        arm()
      }
    })

    arm()
  })
}

/**
 * Fetches customer info without blocking the caller and commits it to the
 * store.
 */
function seedCustomerInfo(set: (partial: Partial<SuperwallStore>) => void): void {
  Promise.resolve()
    .then(() => SuperwallExpoModule.getCustomerInfo())
    .then((customerInfo) => set({ customerInfo }))
    .catch(() => {})
}

/**
 * @category Models
 * @since 0.0.15
 * Interface representing the attributes of a user.
 */
export interface UserAttributes {
  /** The user's alias ID, if set. */
  aliasId: string
  /** The user's application-specific user ID. */
  appUserId: string
  /** The ISO 8601 date string representation of when the application was installed on the user's device. */
  applicationInstalledAt: string
  /** A seed value associated with the user, used for consistent variant assignments in experiments. */
  seed: number
  /** Allows for custom attributes to be set for the user. These can be of any type. */
  [key: string]: any | null
}

/**
 * @category Models
 * @since 0.0.15
 * Options for the `identify` method.
 */
export interface IdentifyOptions {
  /**
   * Determines whether to restore paywall assignments from a previous session for the identified user.
   * If `true`, the SDK attempts to restore the assignments. Defaults to `false`.
   */
  restorePaywallAssignments?: boolean
}

/**
 * @category Store
 * @since 0.0.15
 * Defines the structure of the Superwall store, including its state and actions.
 * This store is managed by Zustand.
 */
export interface SuperwallStore {
  /* -------------------- State -------------------- */
  /** Indicates whether the Superwall SDK has been successfully configured. */
  isConfigured: boolean
  /** Indicates whether the SDK is currently performing a loading operation (e.g., configuring, fetching data). */
  isLoading: boolean
  /** Indicates whether the native event listeners have been initialized. */
  listenersInitialized: boolean

  /**
   * Contains error message if SDK configuration failed, `null` otherwise.
   * When this is set, the SDK is not configured and app should show error UI.
   */
  configurationError: string | null

  /**
   * The native SDK's configuration status: `"PENDING"` until it has fetched its configuration,
   * then `"CONFIGURED"` or `"FAILED"`. Kept current via the native
   * `configurationStatusDidChange` event; use `onConfigurationStatusChange` in
   * `useSuperwallEvents` to be called on each change.
   */
  configurationStatus: ConfigurationStatus

  /**
   * The current user's attributes.
   * `null` if no user is identified or after `reset` is called.
   * `undefined` initially before any user data is fetched or set.
   */
  user?: UserAttributes | null

  /** The current subscription status of the user. */
  subscriptionStatus: SubscriptionStatus

  /**
   * The latest customer purchase and subscription info snapshot.
   * Seeded after configuration completes and kept current via the native
   * `customerInfoDidChange` event. `null` until the first snapshot arrives.
   */
  customerInfo: CustomerInfo | null

  /* -------------------- Internal -------------------- */
  // Internal listener references for cleanup handled inside Provider effect.
  // Not reactive, so we store outside Zustand state to avoid unnecessary rerenders.

  /* -------------------- Actions -------------------- */
  /**
   * Configures the Superwall SDK with the provided API key and options.
   * This must be called before most other SDK functions can be used.
   * @param apiKey - Your Superwall API key.
   * @param options - Optional configuration settings for the SDK.
   * @returns A promise that resolves when configuration is complete.
   * @internal
   */
  configure: (
    apiKey: string,
    options?: PartialSuperwallOptions & {
      /** @deprecated Use manualPurchaseManagement instead */
      manualPurchaseManagment?: boolean
    },
  ) => Promise<void>
  /**
   * Identifies the current user with a unique ID.
   * @param userId - The unique identifier for the user.
   * @param options - Optional parameters for identification.
   * @returns A promise that resolves when identification is complete.
   */
  identify: (userId: string, options?: IdentifyOptions) => Promise<void>
  /**
   * Resets the user's identity and clears all user-specific data, effectively logging them out.
   * @internal
   */
  reset: () => Promise<void>

  /**
   * Registers a placement to potentially show a paywall.
   * The decision to show a paywall is determined by campaign rules and user assignments on the Superwall dashboard.
   * @param placement - The ID of the placement to register.
   * @param params - Optional parameters to pass with the placement.
   * @param handlerId - An optional identifier used to associate specific event handlers (e.g., from `usePlacement`). Defaults to "default".
   * @returns A promise that resolves when the placement registration is complete.
   */
  registerPlacement: (
    placement: string,
    params?: Record<string, any>,
    handlerId?: string | null,
  ) => Promise<void>
  /**
   * Retrieves the presentation result for a given placement.
   * This can be used to understand what would happen if a placement were to be registered, without actually registering it.
   * @param placement - The ID of the placement.
   * @param params - Optional parameters for the placement.
   * @returns A promise that resolves with the presentation result.
   */
  getPresentationResult: (
    placement: string,
    params?: Record<string, any>,
  ) => Promise<PresentationResult>
  /**
   * Programmatically restores purchases.
   * @returns A promise that resolves with a {@link RestorationResultResponse} indicating success or failure.
   */
  restorePurchases: () => Promise<RestorationResultResponse>

  /**
   * Initiates a purchase of a product.
   * @param product - The product identifier, or a product returned by {@link products}. On
   * Android a product object is purchased with its own base plan and offer (its
   * `fullIdentifier`); an identifier can also be a full one, `productId:basePlanId:offerId`.
   * @returns A promise that resolves with a {@link PurchaseResultResponse} indicating the outcome.
   * If the product can't be found or the purchase fails, it resolves with a `failed` result
   * containing the native error message rather than rejecting.
   */
  purchase: (product: string | ProductResponse) => Promise<PurchaseResultResponse>

  /**
   * Fetches product details for an array of product identifiers.
   * @param productIds - An array of product identifiers.
   * @returns A promise that resolves with an array of product objects, in the order requested.
   * Identifiers the store doesn't recognize are omitted.
   */
  products: (productIds: string[]) => Promise<ProductResponse[]>

  /**
   * Android only. Returns the one-time (in-app) products the user currently owns in the
   * `PURCHASED` state, as reported by Google Play Billing. Grant the benefit, then call
   * {@link consume} with each `purchaseToken`. Rejects on iOS.
   * @returns A promise that resolves with the owned, purchased one-time products.
   */
  queryInAppPurchases: () => Promise<OwnedInAppPurchase[]>

  /**
   * Consumes a Google Play purchase token so the item can be purchased again.
   * On iOS this is a no-op that resolves with the given token.
   * @param purchaseToken - The Google Play purchase token to consume.
   * @returns A promise that resolves with the consumed purchase token.
   */
  consume: (purchaseToken: string) => Promise<string>

  /**
   * Dismisses any currently presented Superwall paywall.
   * @returns A promise that resolves when the dismissal is complete.
   */
  dismiss: () => Promise<void>

  /**
   * Presents the Customer Center, a self-service screen where users can view and restore their
   * purchases, manage, cancel or change a subscription, request a refund, manage a web
   * subscription and contact support.
   *
   * Only one Customer Center can be presented at a time; calling this while one is presented
   * does nothing and resolves straight away. It also resolves straight away if the Customer
   * Center couldn't be presented, for example on iOS when there's no view controller to
   * present it from.
   *
   * @param options - Optional per-presentation configuration and callbacks.
   * @returns A promise that resolves once the Customer Center is dismissed.
   */
  presentCustomerCenter: (options?: PresentCustomerCenterOptions) => Promise<void>

  /**
   * Dismisses the Customer Center presented with {@link presentCustomerCenter}.
   * Does nothing if none is presented.
   * @returns A promise that resolves once the Customer Center has been dismissed.
   */
  dismissCustomerCenter: () => Promise<void>

  /**
   * Shows or hides the loading spinner on the currently presented paywall.
   *
   * Useful when a custom paywall action kicks off asynchronous work and you want the
   * paywall to look busy until it finishes. Does nothing if no paywall is presented.
   *
   * @param isHidden - `false` to show the spinner, `true` to hide it again.
   * @returns A promise that resolves once the native SDK has been told.
   *
   * @example
   * usePlacement({
   *   onCustomCallback: async ({ name }) => {
   *     await togglePaywallSpinner(false)
   *     try {
   *       const data = await syncAccount(name)
   *       return { status: "success", data }
   *     } finally {
   *       await togglePaywallSpinner(true)
   *     }
   *   },
   * })
   */
  togglePaywallSpinner: (isHidden: boolean) => Promise<void>

  /**
   * Preloads all paywalls configured in your Superwall dashboard.
   * @returns A promise that resolves when preloading is complete.
   */
  preloadAllPaywalls: () => Promise<void>
  /**
   * Preloads specific paywalls.
   * @param placements - An array of placement IDs for which to preload paywalls.
   * @returns A promise that resolves when preloading is complete.
   */
  preloadPaywalls: (placements: string[]) => Promise<void>

  /**
   * Sets custom attributes for the current user.
   * @param attrs - An object containing the attributes to set.
   * @returns A promise that resolves when attributes are set.
   */
  setUserAttributes: (attrs: Record<string, any | null>) => Promise<void>
  /**
   * Retrieves the current user's attributes.
   * @returns A promise that resolves with the user's attributes.
   */
  getUserAttributes: () => Promise<Record<string, any>>

  /**
   * Sets the logging level for the Superwall SDK.
   * @param level - The desired log level (e.g., "debug", "info", "warn", "error", "none").
   * @returns A promise that resolves when the log level is set.
   */
  setLogLevel: (level: string) => Promise<void>

  /**
   * Sets the locale identifier for the Superwall SDK.
   * This determines the language used when presenting paywalls.
   * Can be changed at runtime without needing to reconfigure.
   * @param localeIdentifier - The locale identifier (e.g., "en", "es", "fr"), or `null` to reset to the device locale.
   * @returns A promise that resolves when the locale identifier is set.
   */
  setLocaleIdentifier: (localeIdentifier: string | null) => Promise<void>

  /**
   * Sets which events Superwall tracks at runtime, for GDPR/data-collection control.
   * @param behavior - The desired event tracking behavior ("all", "superwallOnly", or "none").
   * @returns A promise that resolves when the behavior is set.
   */
  setEventTrackingBehavior: (behavior: EventTrackingBehavior) => Promise<void>

  /**
   * Sets the user's ad consent at runtime, which Superwall passes on with the
   * conversions it uploads to Google Ads and Meta. Overrides the `adConsent` configure option
   * and any TCF consent banner's stored answer.
   * @param consent - The consent for each purpose. A purpose left out is `"granted"`.
   * @returns A promise that resolves when the consent is set.
   */
  setAdConsent: (consent: AdConsent) => Promise<void>

  /**
   * Sets attributes for third-party integrations.
   * @param attributes - Object mapping IntegrationAttribute string values to their IDs
   * @returns A promise that resolves when attributes are set
   */
  setIntegrationAttributes: (attributes: IntegrationAttributes) => Promise<void>

  /**
   * Sets a single attribute for a third-party integration, leaving the others as they are.
   * @param attribute - The integration attribute, such as `"adjustId"`.
   * @param value - The ID, or `null` to remove the attribute.
   * @returns A promise that resolves when the attribute is set
   */
  setIntegrationAttribute: (attribute: IntegrationAttribute, value: string | null) => Promise<void>

  /**
   * Gets the currently set integration attributes.
   * @returns A promise that resolves with the current integration attributes
   */
  getIntegrationAttributes: () => Promise<Record<string, string>>

  /* -------------------- Listener helpers -------------------- */
  /**
   * Initializes native event listeners for the SDK.
   * This is typically called internally by the `SuperwallProvider`.
   * @returns A cleanup function to remove the listeners.
   * @internal
   */
  _initListeners: () => () => void

  setSubscriptionStatus: (status: SubscriptionStatus) => Promise<void>

  getDeviceAttributes: () => Promise<Record<string, any>>

  /**
   * Retrieves the latest customer purchase and subscription info and updates
   * the store's `customerInfo` state.
   *
   * Waits until the SDK has loaded real customer data, so the resolved
   * snapshot is never an unloaded placeholder.
   * @returns A promise that resolves with the {@link CustomerInfo} snapshot.
   */
  getCustomerInfo: () => Promise<CustomerInfo>

  /**
   * Fetches the user's latest entitlements from Superwall, updates the store's `customerInfo`
   * state and returns it. Call it once your server has reported a purchase Superwall can't see
   * on the device, such as a custom product's.
   *
   * iOS only, and only when the app builds SuperwallKit 4.19 or later through Swift Package
   * Manager (see the README). Otherwise, and on Android, the native SDK can't force a refresh
   * and this resolves with the customer info it already has, like {@link getCustomerInfo}.
   * @returns A promise that resolves with the {@link CustomerInfo} snapshot.
   */
  refreshCustomerInfo: () => Promise<CustomerInfo>

  /**
   * Retrieves the App Store / Play Store storefront country code for the current device.
   * @returns A promise that resolves with the storefront country code, or undefined if unavailable.
   */
  getStoreFrontCountryCode: () => Promise<string | undefined>

  /**
   * Retrieves the user's entitlements from Superwall's servers.
   * This includes both active and inactive entitlements.
   * @returns A promise that resolves with the entitlements information.
   */
  getEntitlements: () => Promise<EntitlementsInfo>

  /**
   * Returns the entitlements that the given products unlock.
   * @param productIds - The product identifiers.
   * @returns A promise that resolves with the entitlements, without duplicates.
   */
  getEntitlementsByProductIds: (productIds: string[]) => Promise<Entitlement[]>

  /**
   * Returns the experiment assignments made so far, without confirming the rest.
   * @returns A promise that resolves with the assignments. See {@link Assignment}.
   */
  getAssignments: () => Promise<Assignment[]>

  /**
   * Confirms all experiment assignments and returns them. Assignments may differ when a placement
   * is registered, as audience filters can depend on user, placement and device parameters.
   * @returns A promise that resolves with the assignments. See {@link Assignment}.
   */
  confirmAllAssignments: () => Promise<Assignment[]>

  /**
   * Shows paywalls in the given interface style instead of the system's.
   * @param style - `"LIGHT"` or `"DARK"`, or `null` to follow the system again.
   * @returns A promise that resolves when the style is set.
   */
  setInterfaceStyle: (style: InterfaceStyle | null) => Promise<void>

  /**
   * Returns whether the native SDK has finished configuring. Unlike the other methods, this
   * doesn't wait for configuration.
   * @returns A promise that resolves with `"PENDING"`, `"CONFIGURED"` or `"FAILED"`.
   */
  getConfigurationStatus: () => Promise<ConfigurationStatus>

  /**
   * Handles a Superwall deep link, such as a paywall preview or web checkout link.
   * `SuperwallProvider` already passes the links your app opens through this.
   * @param url - The deep link URL.
   * @returns A promise that resolves with whether Superwall handled the link.
   */
  handleDeepLink: (url: string) => Promise<boolean>

  /**
   * Fetches the Superwall configuration again, so changes made on the dashboard show without
   * restarting the app. Intended for development. On Android the promise resolves once the
   * refresh has started, on iOS once it has finished.
   * @returns A promise that resolves as described.
   */
  refreshConfiguration: () => Promise<void>

  /**
   * Returns the info of the paywall on screen, or else the most recently presented one.
   * @returns A promise that resolves with the {@link PaywallInfo}, or `null` if none.
   */
  getLatestPaywallInfo: () => Promise<PaywallInfo | null>

  /**
   * Returns whether a paywall is presented.
   * @returns A promise that resolves with `true` if a paywall is on screen.
   */
  getIsPaywallPresented: () => Promise<boolean>

  /**
   * Returns the current user's ID: the one passed to `identify`, or an anonymous ID.
   * @returns A promise that resolves with the user ID.
   */
  getUserId: () => Promise<string>

  /**
   * Returns whether the user has been identified with `identify`.
   * @returns A promise that resolves with `true` if the user is logged in.
   */
  getIsLoggedIn: () => Promise<boolean>

  /**
   * Returns which events Superwall tracks.
   * @returns A promise that resolves with the current {@link EventTrackingBehavior}.
   */
  getEventTrackingBehavior: () => Promise<EventTrackingBehavior>

  /**
   * Returns the ad consent Superwall passes on with the conversions it uploads. If it was never
   * set, both purposes are `"granted"`.
   * @returns A promise that resolves with the current {@link AdConsent}.
   */
  getAdConsent: () => Promise<Required<AdConsent>>

  /**
   * Returns the locale identifier set with `setLocaleIdentifier` or the `localeIdentifier` option.
   * @returns A promise that resolves with the locale identifier, or `null` for the device locale.
   */
  getLocaleIdentifier: () => Promise<string | null>

  /**
   * Returns the SDK's log level.
   * @returns A promise that resolves with the current {@link LogLevel}.
   */
  getLogLevel: () => Promise<LogLevel>

  /**
   * Replaces products on every paywall by product name, such as `{ primary: "com.app.yearly" }`,
   * overriding the `overrideProductsByName` paywall option.
   * @param overrides - Product identifiers keyed by the product names set in the paywall editor.
   * @returns A promise that resolves when the overrides are set.
   */
  setOverrideProductsByName: (overrides: Record<string, string>) => Promise<void>

  /**
   * Returns the products that replace paywall products by name.
   * @returns A promise that resolves with the product identifiers keyed by product name.
   */
  getOverrideProductsByName: () => Promise<Record<string, string>>

  /**
   * Cancels the local notifications Superwall has scheduled, such as free trial reminders.
   * @returns A promise that resolves once the native SDK has been told.
   */
  cancelAllScheduledNotifications: () => Promise<void>

  /**
   * Android only. Gives Superwall the device's Firebase Cloud Messaging registration token, so it
   * can send push notifications to this device. Call it on launch and whenever FCM issues a new
   * token. Does nothing on iOS.
   * @param token - The FCM registration token.
   * @returns A promise that resolves once the native SDK has been told.
   */
  setPushToken: (token: string) => Promise<void>

  /**
   * Android only. Stops Superwall sending push notifications to this device, until
   * {@link optInToNotifications} is called. Remembered across launches, `reset` and `identify`.
   * Does nothing on iOS.
   * @returns A promise that resolves once the native SDK has been told.
   */
  optOutOfNotifications: () => Promise<void>

  /**
   * Android only. Lets Superwall send push notifications to this device again after
   * {@link optOutOfNotifications}. Devices are opted in by default. Does nothing on iOS.
   * @returns A promise that resolves once the native SDK has been told.
   */
  optInToNotifications: () => Promise<void>

  /**
   * Android only. Tells Superwall the user opened a notification, so it can measure the
   * notifications it sends. Pass the data of a notification the user tapped; never call it when
   * a message is merely delivered. Notifications Superwall didn't send are ignored. Does nothing
   * on iOS.
   * @param data - The notification's data.
   * @returns A promise that resolves once the native SDK has been told.
   */
  handleNotificationOpen: (data: Record<string, string>) => Promise<void>

  /**
   * iOS only. Removes all of your app's pending local notifications except the ones Superwall
   * scheduled. Does nothing on Android.
   * @returns A promise that resolves once the native SDK has been told.
   */
  removeAllPendingNonSuperwallNotificationRequests: () => Promise<void>

  /**
   * iOS only. Removes the notifications Superwall delivered from Notification Center. Doesn't
   * affect notifications that are scheduled but not yet delivered. Does nothing on Android.
   * @returns A promise that resolves once the native SDK has been told.
   */
  removeAllDeliveredSuperwallNotifications: () => Promise<void>

  /**
   * iOS only. Removes all of your app's delivered notifications from Notification Center except
   * Superwall's. Does nothing on Android.
   * @returns A promise that resolves once the native SDK has been told.
   */
  removeAllDeliveredNonSuperwallNotifications: () => Promise<void>

  /**
   * iOS only. Returns the entitlements granted with {@link setGrantedEntitlements}. Always an
   * empty array on Android.
   * @returns A promise that resolves with the granted entitlements.
   */
  getGrantedEntitlements: () => Promise<Entitlement[]>

  /**
   * iOS only. Grants entitlements that your own backend gives out and Superwall can't observe,
   * such as a promotional grant. The SDK merges them into the subscription status alongside
   * device and web entitlements. Don't use it if you set the full subscription status yourself
   * with a purchase controller.
   *
   * Each call replaces the previous set; pass an empty array to revoke them. They persist
   * across app launches, `reset` and `identify`, so set them again after either if the new
   * user shouldn't inherit them. Does nothing on Android.
   * @param entitlements - The entitlements to grant, or their identifiers.
   * @returns A promise that resolves when the entitlements are set.
   */
  setGrantedEntitlements: (entitlements: (string | { id: string })[]) => Promise<void>

  /**
   * Android only. Shows an alert over the presented paywall. Does nothing on iOS.
   * @param options - The alert's title, message and button titles.
   * @returns A promise that resolves with `true` if the user tapped the action button, and
   * `false` if they closed the alert or there was no paywall to show it over (always the case
   * on iOS).
   */
  showAlert: (options?: ShowAlertOptions) => Promise<boolean>

  /**
   * Android only. Opens the web page where the user can restore purchases made on the web.
   * Does nothing on iOS.
   * @returns A promise that resolves once the native SDK has been told.
   */
  openRestoreOnWeb: () => Promise<void>

  /**
   * Android only. Returns the account ID Superwall passes to Google Play with purchases when
   * `passIdentifiersToPlayStore` is on.
   * @returns A promise that resolves with the ID, or `null` on iOS.
   */
  getExternalAccountId: () => Promise<string | null>

  /**
   * Android only. Reports a purchase made outside of Superwall, for observer mode
   * (`shouldObservePurchases`). Report `purchaseWillBegin` before launching the billing flow,
   * then `purchaseResult` with what Play Billing returned, or `purchaseError` if it failed
   * before that. Does nothing on iOS, where the SDK observes StoreKit itself.
   * @param state - The stage of the purchase. See {@link PurchasingObserverState}.
   * @returns A promise that resolves once the SDK has been told. Rejects if the product can't
   * be found.
   */
  observe: (state: PurchasingObserverState) => Promise<void>

  /**
   * Android only. Resets the native SDK and clears its instance, so it can be configured
   * again, for example with another API key. Afterwards the store is back to its
   * not-configured state until `configure` runs again. Does nothing on iOS, which has no way
   * to tear the SDK down.
   * @returns A promise that resolves with `true` if an SDK instance was torn down.
   */
  teardown: () => Promise<boolean>

  /**
   * Android only. Dismisses the presented paywall with the "for next paywall" close reason,
   * which you use when another paywall is about to be presented: the feature block of a
   * non-gated placement doesn't run. Does nothing on iOS.
   * @returns A promise that resolves when the dismissal is complete.
   */
  dismissForNextPaywall: () => Promise<void>
}

/**
 * @category Store
 * @since 0.0.15
 * Zustand store for Superwall SDK state and actions.
 * @internal
 */
export const useSuperwallStore = create<SuperwallStore>((set, get) => ({
  /* -------------------- State -------------------- */
  isConfigured: false,
  isLoading: false,
  listenersInitialized: false,
  configurationError: null,
  configurationStatus: "PENDING",

  user: null,
  subscriptionStatus: {
    status: "UNKNOWN",
  },
  customerInfo: null,

  /* -------------------- Actions -------------------- */
  configure: async (apiKey, options) => {
    set({ isLoading: true, configurationError: null })

    try {
      const {
        manualPurchaseManagement,
        manualPurchaseManagment,
        localResources,
        customerCenter,
        adConsent,
        ...restOptions
      } = options || {}

      // Support both spellings for backward compatibility
      const isManualPurchaseManagement =
        manualPurchaseManagement ?? manualPurchaseManagment ?? false

      const resolvedLocalResources = await resolveLocalResources(localResources)

      // Deep merge partial options with defaults to ensure all required fields are present
      const mergedOptions = {
        ...DefaultSuperwallOptions,
        ...restOptions,
        ...(resolvedLocalResources ? { localResources: resolvedLocalResources } : {}),
        ...(customerCenter
          ? { customerCenter: customerCenterConfigurationToJson(customerCenter) }
          : {}),
        ...(adConsent ? { adConsent: filterUndefined(adConsent) } : {}),
        ...(typeof restOptions.networkEnvironment === "object"
          ? { networkEnvironment: filterUndefined(restOptions.networkEnvironment) }
          : {}),
        paywalls: {
          ...DefaultSuperwallOptions.paywalls,
          // Strip undefined so an explicitly undefined field (e.g. `presentationHost`) falls
          // back to the default instead of reaching the Android bridge, which can't convert it
          ...filterUndefined(restOptions.paywalls ?? {}),
          restoreFailed: {
            ...DefaultSuperwallOptions.paywalls.restoreFailed,
            ...restOptions.paywalls?.restoreFailed,
          },
        },
        logging: {
          ...DefaultSuperwallOptions.logging,
          ...restOptions.logging,
        },
      }

      await SuperwallExpoModule.configure(
        apiKey,
        mergedOptions,
        isManualPurchaseManagement,
        pkg.version,
      )

      const currentUser = await SuperwallExpoModule.getUserAttributes()
      const subscriptionStatus = await SuperwallExpoModule.getSubscriptionStatus()

      set({
        isConfigured: true,
        isLoading: false,
        configurationError: null,
        user: currentUser as UserAttributes,
        subscriptionStatus,
      })

      // Seed customer info without blocking configuration
      seedCustomerInfo(set)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error)
      set({
        isLoading: false,
        configurationError: errorMessage,
      })
      // Don't throw - let developers handle via state or callback
    }
  },
  identify: async (userId, options) => {
    await awaitConfigured()

    // The previous identity's purchases must not leak into the new one.
    set({ customerInfo: null })

    await SuperwallExpoModule.identify(userId, options)

    // TODO: Instead of setting users after identify, we should set this based on an event
    await new Promise((resolve) => setTimeout(resolve, 0))

    const [currentUser, subscriptionStatus] = await Promise.all([
      SuperwallExpoModule.getUserAttributes(),
      SuperwallExpoModule.getSubscriptionStatus(),
    ])

    set({ user: currentUser as UserAttributes, subscriptionStatus })
    seedCustomerInfo(set)
  },
  reset: async () => {
    await awaitConfigured()

    // The signed-out identity's purchases must not survive the reset.
    set({ customerInfo: null })

    await SuperwallExpoModule.reset()

    const currentUser = await SuperwallExpoModule.getUserAttributes()
    const subscriptionStatus = await SuperwallExpoModule.getSubscriptionStatus()

    set({ user: currentUser as UserAttributes, subscriptionStatus })
    seedCustomerInfo(set)
  },
  registerPlacement: async (placement, params, handlerId = "default") => {
    await awaitConfigured()
    await SuperwallExpoModule.registerPlacement(placement, params, handlerId)
  },
  getPresentationResult: async (placement, params) => {
    await awaitConfigured()
    return SuperwallExpoModule.getPresentationResult(placement, params)
  },
  restorePurchases: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.restorePurchases()
  },
  purchase: async (product) => {
    await awaitConfigured()
    return SuperwallExpoModule.purchase(productIdentifierForPurchase(product))
  },
  products: async (productIds) => {
    await awaitConfigured()
    return SuperwallExpoModule.products(productIds)
  },
  queryInAppPurchases: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.queryInAppPurchases()
  },
  consume: async (purchaseToken) => {
    await awaitConfigured()
    return SuperwallExpoModule.consume(purchaseToken)
  },
  dismiss: async () => {
    await awaitConfigured()
    await SuperwallExpoModule.dismiss()
  },
  presentCustomerCenter: async (options) => {
    await awaitConfigured()
    await presentCustomerCenter(options)
  },
  dismissCustomerCenter: async () => {
    await awaitConfigured()
    await dismissCustomerCenter()
  },
  togglePaywallSpinner: async (isHidden) => {
    await awaitConfigured()
    SuperwallExpoModule.togglePaywallSpinner(isHidden)
  },
  preloadAllPaywalls: async () => {
    await awaitConfigured()
    await SuperwallExpoModule.preloadAllPaywalls()
  },
  preloadPaywalls: async (placements) => {
    await awaitConfigured()
    await SuperwallExpoModule.preloadPaywalls(placements)
  },
  setUserAttributes: async (attrs) => {
    await awaitConfigured()
    await SuperwallExpoModule.setUserAttributes(filterUndefined(attrs))

    const currentUser = await SuperwallExpoModule.getUserAttributes()
    set({ user: currentUser as UserAttributes })
  },
  getUserAttributes: async () => {
    await awaitConfigured()
    const attributes = await SuperwallExpoModule.getUserAttributes()
    set({ user: attributes as UserAttributes })
    return attributes
  },
  setLogLevel: async (level) => {
    await awaitConfigured()
    await SuperwallExpoModule.setLogLevel(level)
  },
  setEventTrackingBehavior: async (behavior) => {
    await awaitConfigured()
    await SuperwallExpoModule.setEventTrackingBehavior(behavior)
  },
  setAdConsent: async (consent) => {
    await awaitConfigured()
    if (typeof SuperwallExpoModule.setAdConsent !== "function") return
    SuperwallExpoModule.setAdConsent(filterUndefined(consent) as Record<string, string>)
  },

  setLocaleIdentifier: async (localeIdentifier) => {
    await awaitConfigured()
    await SuperwallExpoModule.setLocaleIdentifier(localeIdentifier)
  },

  setIntegrationAttributes: async (attributes) => {
    await awaitConfigured()
    await SuperwallExpoModule.setIntegrationAttributes(attributes)

    const currentUser = await SuperwallExpoModule.getUserAttributes()
    set({ user: currentUser as UserAttributes })
  },

  setIntegrationAttribute: async (attribute, value) => {
    await awaitConfigured()
    await SuperwallExpoModule.setIntegrationAttribute(attribute, value)

    const currentUser = await SuperwallExpoModule.getUserAttributes()
    set({ user: currentUser as UserAttributes })
  },

  getIntegrationAttributes: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.getIntegrationAttributes()
  },

  setSubscriptionStatus: async (status) => {
    await awaitConfigured()
    await SuperwallExpoModule.setSubscriptionStatus(status)
  },
  getDeviceAttributes: async () => {
    await awaitConfigured()
    const attributes = await SuperwallExpoModule.getDeviceAttributes()
    return attributes
  },
  getCustomerInfo: async () => {
    await awaitConfigured()
    const customerInfo = await SuperwallExpoModule.getCustomerInfo()
    set({ customerInfo })
    return customerInfo
  },
  refreshCustomerInfo: async () => {
    await awaitConfigured()
    const customerInfo = await SuperwallExpoModule.refreshCustomerInfo()
    set({ customerInfo })
    return customerInfo
  },
  getStoreFrontCountryCode: async () => {
    await awaitConfigured()
    const attributes = await SuperwallExpoModule.getDeviceAttributes()
    return attributes?.["storeFrontCountryCode"] as string | undefined
  },
  getEntitlements: async () => {
    await awaitConfigured()
    const entitlements = await SuperwallExpoModule.getEntitlements()
    return entitlements as EntitlementsInfo
  },
  getEntitlementsByProductIds: async (productIds) => {
    await awaitConfigured()
    return SuperwallExpoModule.getEntitlementsByProductIds(productIds)
  },
  getAssignments: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.getAssignments()
  },
  confirmAllAssignments: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.confirmAllAssignments()
  },
  setInterfaceStyle: async (style) => {
    await awaitConfigured()
    SuperwallExpoModule.setInterfaceStyle(style ?? undefined)
  },
  getConfigurationStatus: async () => {
    return (await SuperwallExpoModule.getConfigurationStatus()) as ConfigurationStatus
  },
  handleDeepLink: async (url) => {
    await awaitConfigured()
    return SuperwallExpoModule.handleDeepLink(url)
  },
  refreshConfiguration: async () => {
    await awaitConfigured()
    await SuperwallExpoModule.refreshConfiguration()
  },
  getLatestPaywallInfo: async () => {
    await awaitConfigured()
    return (await SuperwallExpoModule.getLatestPaywallInfo()) ?? null
  },
  getIsPaywallPresented: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.getIsPaywallPresented()
  },
  getUserId: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.getUserId()
  },
  getIsLoggedIn: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.getIsLoggedIn()
  },
  getEventTrackingBehavior: async () => {
    await awaitConfigured()
    return (await SuperwallExpoModule.getEventTrackingBehavior()) as EventTrackingBehavior
  },
  getAdConsent: async () => {
    await awaitConfigured()
    return (await SuperwallExpoModule.getAdConsent()) as Required<AdConsent>
  },
  getLocaleIdentifier: async () => {
    await awaitConfigured()
    return (await SuperwallExpoModule.getLocaleIdentifier()) ?? null
  },
  getLogLevel: async () => {
    await awaitConfigured()
    return (await SuperwallExpoModule.getLogLevel()) as LogLevel
  },
  setOverrideProductsByName: async (overrides) => {
    await awaitConfigured()
    SuperwallExpoModule.setOverrideProductsByName(overrides)
  },
  getOverrideProductsByName: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.getOverrideProductsByName()
  },
  cancelAllScheduledNotifications: async () => {
    await awaitConfigured()
    SuperwallExpoModule.cancelAllScheduledNotifications()
  },
  setPushToken: async (token) => {
    await awaitConfigured()
    SuperwallExpoModule.setPushToken(token)
  },
  optOutOfNotifications: async () => {
    await awaitConfigured()
    SuperwallExpoModule.optOutOfNotifications()
  },
  optInToNotifications: async () => {
    await awaitConfigured()
    SuperwallExpoModule.optInToNotifications()
  },
  handleNotificationOpen: async (data) => {
    await awaitConfigured()
    SuperwallExpoModule.handleNotificationOpen(data)
  },
  removeAllPendingNonSuperwallNotificationRequests: async () => {
    await awaitConfigured()
    SuperwallExpoModule.removeAllPendingNonSuperwallNotificationRequests()
  },
  removeAllDeliveredSuperwallNotifications: async () => {
    await awaitConfigured()
    SuperwallExpoModule.removeAllDeliveredSuperwallNotifications()
  },
  removeAllDeliveredNonSuperwallNotifications: async () => {
    await awaitConfigured()
    SuperwallExpoModule.removeAllDeliveredNonSuperwallNotifications()
  },
  getGrantedEntitlements: async () => {
    await awaitConfigured()
    return SuperwallExpoModule.getGrantedEntitlements()
  },
  setGrantedEntitlements: async (entitlements) => {
    await awaitConfigured()
    await SuperwallExpoModule.setGrantedEntitlements(
      entitlements.map((entitlement) => ({
        id: typeof entitlement === "string" ? entitlement : entitlement.id,
      })),
    )
  },
  showAlert: async (options = {}) => {
    await awaitConfigured()
    return SuperwallExpoModule.showAlert(
      options.title ?? null,
      options.message ?? null,
      options.actionTitle ?? null,
      options.closeActionTitle ?? null,
    )
  },
  openRestoreOnWeb: async () => {
    await awaitConfigured()
    SuperwallExpoModule.openRestoreOnWeb()
  },
  getExternalAccountId: async () => {
    await awaitConfigured()
    return (await SuperwallExpoModule.getExternalAccountId()) ?? null
  },
  observe: async (state) => {
    await awaitConfigured()
    await SuperwallExpoModule.observe(purchasingObserverStateToJson(state))
  },
  teardown: async () => {
    await awaitConfigured()
    const didTearDown = await SuperwallExpoModule.teardown()
    if (didTearDown) {
      // The native SDK is gone, so calls wait for the next `configure` again.
      set({
        isConfigured: false,
        configurationStatus: "PENDING",
        user: null,
        subscriptionStatus: { status: "UNKNOWN" },
        customerInfo: null,
      })
    }
    return didTearDown
  },
  dismissForNextPaywall: async () => {
    await awaitConfigured()
    await SuperwallExpoModule.dismissForNextPaywall()
  },

  /* -------------------- Listener helpers -------------------- */
  _initListeners: (): (() => void) => {
    // Use get() to read the state from within the store
    if (get().listenersInitialized) {
      console.warn("[Superwall] Listeners already initialized. Skipping.")
      return () => {} // Return no-op cleanup
    }

    const subscriptions: { remove: () => void }[] = []

    subscriptions.push(
      SuperwallExpoModule.addListener(
        "subscriptionStatusDidChange",
        ({ to }: { to: SubscriptionStatus }) => {
          set({ subscriptionStatus: to })
        },
      ),
    )

    subscriptions.push(
      SuperwallExpoModule.addListener("customerInfoDidChange", ({ to }: { to: CustomerInfo }) => {
        set({ customerInfo: to })
      }),
    )

    subscriptions.push(
      SuperwallExpoModule.addListener(
        "configurationStatusDidChange",
        ({ status }: { status: ConfigurationStatus }) => {
          set({ configurationStatus: status })
        },
      ),
    )

    // Listen for configuration events
    subscriptions.push(
      SuperwallExpoModule.addListener("handleSuperwallEvent", ({ eventInfo }) => {
        if (eventInfo.event.event === "configFail") {
          set({
            configurationError: "Failed to load Superwall configuration",
            isLoading: false,
          })
        } else if (eventInfo.event.event === "configRefresh") {
          // Clear any previous errors on successful refresh
          set({ configurationError: null })
        }
      }),
    )

    set({ listenersInitialized: true })
    console.log("[Superwall] Initialized listeners", subscriptions.length)

    return (): void => {
      console.log("[Superwall] Cleaning up listeners", subscriptions.length)
      // biome-ignore lint/suspicious/useIterableCallbackReturn: forEach is used for side effects only
      subscriptions.forEach((s) => s.remove())
      // Reset the state on cleanup
      set({ listenersInitialized: false })
    }
  },
}))

/**
 * @category Store
 * @since 0.0.15
 * Public interface for the Superwall store, excluding internal methods.
 */
export type PublicSuperwallStore = Omit<SuperwallStore, "configure" | "reset" | "_initListeners">

export const SuperwallContext = createContext<boolean>(false)

/**
 * @category Hooks
 * @since 0.0.15
 * Core React hook for interacting with the Superwall SDK.
 *
 * This hook provides access to the Superwall store, which includes SDK state
 * (like configuration status, user information, subscription status) and actions
 * (like `identify`, `reset`, `registerPlacement`).
 *
 * It must be used within a component that is a descendant of `<SuperwallProvider />`.
 *
 * @template T - Optional type parameter for the selected state. Defaults to the entire `PublicSuperwallStore`.
 * @param selector - An optional function to select a specific slice of the store's state.
 *                   This is useful for performance optimization, as components will only re-render
 *                   if the selected part of the state changes. Uses shallow equality checking
 *                   via `zustand/shallow`. If omitted, the entire store is returned.
 * @returns The selected slice of the Superwall store state, or the entire store if no selector is provided.
 * @throws Error if used outside of a `SuperwallProvider`.
 *
 * @example
 * // Get the entire store
 * const superwall = useSuperwall();
 * console.log(superwall.isConfigured);
 * superwall.identify("user_123");
 *
 * @example
 * // Select specific state properties
 * const { user, subscriptionStatus } = useSuperwall(state => ({
 *   user: state.user,
 *   subscriptionStatus: state.subscriptionStatus,
 * }));
 * console.log(user?.appUserId, subscriptionStatus?.status);
 */
export function useSuperwall<T = PublicSuperwallStore>(selector?: (state: SuperwallStore) => T): T {
  const inProvider = useContext(SuperwallContext)
  if (!inProvider) {
    throw new Error("useSuperwall must be used within a SuperwallProvider")
  }

  const identity = (state: SuperwallStore) => state as unknown as T
  // biome-ignore lint/correctness/useHookAtTopLevel: good here
  return useSuperwallStore(selector ? useShallow(selector) : identity)
}
