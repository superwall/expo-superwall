import { Assignment } from "./lib/Assigments"
import { ConfigurationStatus } from "./lib/ConfigurationStatus"
import { EntitlementsInfo } from "./lib/EntitlementsInfo"
import { IdentityOptions } from "./lib/IdentityOptions"
import type { InterfaceStyle } from "./lib/InterfaceStyle"
import type { LogLevel } from "./lib/LogLevel"
import { PaywallInfo } from "./lib/PaywallInfo"
import type { PaywallPresentationHandler } from "./lib/PaywallPresentationHandler"
import { fromJson as paywallResultFromJson } from "./lib/PaywallResult"
import { PaywallSkippedReason } from "./lib/PaywallSkippedReason"
import type { PresentationResult } from "./lib/PresentationResult"
import type { PurchaseController } from "./lib/PurchaseController"
import { RedemptionResults } from "./lib/RedemptionResults"
import { SubscriptionStatus } from "./lib/SubscriptionStatus"
import type { SuperwallDelegate } from "./lib/SuperwallDelegate"
import { SuperwallEventInfo } from "./lib/SuperwallEventInfo"
import type { SuperwallOptions } from "./lib/SuperwallOptions"

import { EventEmitter } from "expo"
import { version } from "../../package.json"
import SuperwallExpoModule from "../SuperwallExpoModule"

export { ComputedPropertyRequest } from "./lib/ComputedPropertyRequest"
export { Experiment } from "./lib/Experiment"
export { FeatureGatingBehavior } from "./lib/FeatureGatingBehavior"
export { IdentityOptions } from "./lib/IdentityOptions"
export { LocalNotification } from "./lib/LocalNotification"
export { LogLevel } from "./lib/LogLevel"
export { LogScope } from "./lib/LogScope"
export { PaywallCloseReason } from "./lib/PaywallCloseReason"
export { PaywallInfo } from "./lib/PaywallInfo"
export { Product } from "./lib/Product"
export { PurchaseController } from "./lib/PurchaseController"
export { SubscriptionStatus } from "./lib/SubscriptionStatus"
export { PurchaseResult } from "./lib/PurchaseResult"
export {
  PurchaseResultPurchased,
  PurchaseResultPending,
  PurchaseResultCancelled,
  PurchaseResultFailed,
  PurchaseResultRestored,
} from "./lib/PurchaseResult"
export { RestorationResult } from "./lib/RestorationResult"
export { InterfaceStyle } from "./lib/InterfaceStyle"
export { ConfigurationStatus } from "./lib/ConfigurationStatus"
export { SuperwallDelegate } from "./lib/SuperwallDelegate"
export { SuperwallEventInfo, EventType } from "./lib/SuperwallEventInfo"
export { SuperwallOptions } from "./lib/SuperwallOptions"
export { Survey } from "./lib/Survey"
export { TriggerResult } from "./lib/TriggerResult"
export {
  PaywallOptions,
  TransactionBackgroundView,
} from "./lib/PaywallOptions"
export { PaywallPresentationHandler } from "./lib/PaywallPresentationHandler"
export { PaywallPresentationRequestStatus } from "./lib/PaywallPresentationRequestStatus"
export {
  PaywallSkippedReason,
  PaywallSkippedReasonPlacementNotFound,
  PaywallSkippedReasonHoldout,
  PaywallSkippedReasonNoAudienceMatch,
  PaywallSkippedReasonUserIsSubscribed,
} from "./lib/PaywallSkippedReason"
export { RestoreType } from "./lib/RestoreType"
export { EntitlementsInfo } from "./lib/EntitlementsInfo"
export * from "./lib/RedemptionResults"

interface UserAttributes {
  [key: string]: any
}

export default class Superwall {
  static purchaseController?: PurchaseController
  private static delegate?: SuperwallDelegate
  private static _superwall = new Superwall()

  private static configEmitter = new EventEmitter<{
    configured: (isConfigured: boolean) => void
  }>()
  private static didConfigure = false
  private presentationHandlers: Map<string, PaywallPresentationHandler> = new Map()
  subscriptionStatusEmitter = new EventEmitter<{
    change: (status: SubscriptionStatus) => void
  }>()

  private static setDidConfigure(didConfigure: boolean) {
    Superwall.didConfigure = didConfigure
    // Emit an event when the bridged state is true
    if (didConfigure) {
      Superwall.configEmitter.emit("configured", didConfigure)
    }
  }

  private async awaitConfig(): Promise<void> {
    if (Superwall.didConfigure) {
      return
    }

    await new Promise<void>((resolve) => {
      Superwall.configEmitter.addListener("configured", () => {
        resolve()
      })
    })
  }

  private constructor() {
    SuperwallExpoModule.addListener("onPurchase", async (data) => {
      if (data.platform === "ios") {
        const purchaseResult = await Superwall.purchaseController?.purchaseFromAppStore(
          data.productId,
        )
        if (purchaseResult == null) {
          return
        }
        await SuperwallExpoModule.didPurchase(purchaseResult.toJSON())
        return
      }

      if (data.platform === "android") {
        const purchaseResult = await Superwall.purchaseController?.purchaseFromGooglePlay(
          data.productId,
          data.basePlanId,
          data.offerId,
        )
        if (purchaseResult == null) {
          return
        }
        await SuperwallExpoModule.didPurchase(purchaseResult.toJSON())
      }
    })

    SuperwallExpoModule.addListener("onPurchaseRestore", async () => {
      const restorationResult = await Superwall.purchaseController?.restorePurchases()
      if (restorationResult == null) {
        return
      }
      await SuperwallExpoModule.didRestore(restorationResult.toJson())
    })

    SuperwallExpoModule.addListener("onPaywallPresent", (data) => {
      const handler = this.presentationHandlers.get(data.handlerId)
      if (!handler || !handler.onPresentHandler) {
        return
      }

      const paywallInfo = PaywallInfo.fromJson(data.paywallInfoJson)
      handler.onPresentHandler(paywallInfo)
    })
    SuperwallExpoModule.addListener("onPaywallDismiss", (data) => {
      const handler = this.presentationHandlers.get(data.handlerId)

      if (!handler || !handler.onDismissHandler) {
        return
      }

      const info = PaywallInfo.fromJson(data.paywallInfoJson)
      const result = paywallResultFromJson(data.result)
      handler.onDismissHandler(info, result)
    })

    SuperwallExpoModule.addListener("onPaywallError", (data) => {
      const handler = this.presentationHandlers.get(data.handlerId)

      if (!handler || !handler.onErrorHandler) {
        return
      }

      handler.onErrorHandler(data.errorString)
    })

    SuperwallExpoModule.addListener("onPaywallSkip", (data) => {
      const handler = this.presentationHandlers.get(data.handlerId)

      if (!handler || !handler.onSkipHandler) {
        return
      }

      const skippedReason = PaywallSkippedReason.fromJson(data.skippedReason)
      handler.onSkipHandler(skippedReason)
    })

    SuperwallExpoModule.addListener("onPaywallPresent", (data) => {
      const handler = this.presentationHandlers.get(data.handlerId)

      if (!handler || !handler.onPresentHandler) {
        return
      }

      const info = PaywallInfo.fromJson(data.paywallInfoJson)
      handler.onPresentHandler(info)
    })

    // MARK: - SuperwallDelegate Listeners
    SuperwallExpoModule.addListener("subscriptionStatusDidChange", async (data) => {
      Superwall.delegate?.subscriptionStatusDidChange(data.from, data.to)
    })

    SuperwallExpoModule.addListener("handleSuperwallEvent", async (data) => {
      const eventInfo = SuperwallEventInfo.fromJson(data.eventInfo)
      Superwall.delegate?.handleSuperwallEvent(eventInfo)
    })

    SuperwallExpoModule.addListener("handleCustomPaywallAction", async (data) => {
      const name = data.name
      Superwall.delegate?.handleCustomPaywallAction(name)
    })

    SuperwallExpoModule.addListener("willDismissPaywall", async (data) => {
      const info = PaywallInfo.fromJson(data.info)
      Superwall.delegate?.willDismissPaywall(info)
    })

    SuperwallExpoModule.addListener("willPresentPaywall", async (data) => {
      const info = PaywallInfo.fromJson(data.info)
      Superwall.delegate?.willPresentPaywall(info)
    })

    SuperwallExpoModule.addListener("didDismissPaywall", async (data) => {
      const info = PaywallInfo.fromJson(data.info)
      Superwall.delegate?.didDismissPaywall(info)
    })

    SuperwallExpoModule.addListener("didPresentPaywall", async (data) => {
      const info = PaywallInfo.fromJson(data.info)
      Superwall.delegate?.didPresentPaywall(info)
    })

    SuperwallExpoModule.addListener("handleLog", async (data) => {
      Superwall.delegate?.handleLog(
        data.level,
        data.scope,
        data.message || undefined,
        data.info,
        data.error,
      )
    })

    SuperwallExpoModule.addListener("paywallWillOpenDeepLink", async (data) => {
      const url = new URL(data.url)
      Superwall.delegate?.paywallWillOpenDeepLink(url)
    })

    SuperwallExpoModule.addListener("paywallWillOpenURL", async (data) => {
      const url = new URL(data.url)
      Superwall.delegate?.paywallWillOpenURL(url)
    })

    SuperwallExpoModule.addListener("willRedeemLink", async () => {
      Superwall.delegate?.willRedeemLink()
    })

    SuperwallExpoModule.addListener("didRedeemLink", async (data) => {
      const result = RedemptionResults.fromJson(data)
      Superwall.delegate?.didRedeemLink(result)
    })
  }

  // TODO: Not sure if this is needed
  //   private async observeSubscriptionStatus() {
  //     await SuperwallReactNative.observeSubscriptionStatus()
  //     SuperwallExpoModule.addListener("observeSubscriptionStatus", async (data) => {
  //       const status = SubscriptionStatus.fromJson(data)
  //       this.subscriptionStatusEmitter.emit("change", status)
  //     })
  //   }
  /**
   * Returns the configured shared instance of `Superwall`.
   *
   * **Warning:** You must call {@link Superwall.configure} to initialize `Superwall`
   * before accessing this shared instance.
   *
   * @returns {Superwall} The shared `Superwall` instance.
   */
  static get shared(): Superwall {
    return Superwall._superwall
  }

  /**
   * Configures a shared instance of `Superwall` for use throughout your app.
   *
   * Call this as soon as your app starts to initialize the Superwall SDK.
   * Check out [Configuring the SDK](https://docs.superwall.com/docs/configuring-the-sdk) for information about how to configure the SDK.
   *
   * @param {object} config - Configuration object.
   * @param {string} config.apiKey - Your lib API Key that you can get from the Superwall dashboard settings.
   *   If you don't have an account, you can [sign up for free](https://superwall.com/sign-up).
   * @param {SuperwallOptions} [config.options] - An optional object which allows you to customize the appearance and behavior
   *   of the paywall.
   * @param {PurchaseController} [config.purchaseController] - An optional object that conforms to `PurchaseController`.
   *   Implement this if you'd like to handle all subscription-related logic yourself. You'll need to also set the `subscriptionStatus`
   *   every time the user's entitlements change. You can read more about that in [Purchases and Subscription Status](https://docs.superwall.com/docs/advanced-configuration).
   *   If omitted, Superwall will handle all subscription-related logic itself.
   * @param {() => void} [config.completion] - An optional completion handler that lets you know when Superwall has finished configuring.
   *
   * @returns {Promise<Superwall>} The configured `Superwall` instance.
   */
  static async configure({
    apiKey,
    options,
    purchaseController,
    completion,
  }: {
    apiKey: string
    options?: SuperwallOptions
    purchaseController?: PurchaseController
    completion?: () => void
  }): Promise<Superwall> {
    Superwall.purchaseController = purchaseController
    Superwall.purchaseController = purchaseController
    await SuperwallExpoModule.configure(apiKey, options?.toJson(), `${version}compat`)

    completion?.()

    Superwall.setDidConfigure(true)

    return Superwall._superwall
  }

  /**
   * Creates an account with Superwall by linking the provided `userId` to Superwall's automatically generated alias.
   *
   * Call this function as soon as you have a valid `userId`.
   *
   * @param {Object} config - The identification configuration object.
   * @param {string} config.userId - Your user's unique identifier as defined by your backend system.
   * @param {IdentityOptions} [config.options] - An optional {@link IdentityOptions} object. You can set the
   *   {@link IdentityOptions.restorePaywallAssignments} property to `true` to instruct the SDK to wait to restore paywall assignments
   *   from the server before presenting any paywalls. This option should be used only in advanced cases
   *   (e.g., when users frequently switch accounts or reinstall the app).
   *
   * @returns {Promise<void>} A promise that resolves once the identification process is complete.
   */
  async identify({
    userId,
    options,
  }: {
    userId: string
    options?: IdentityOptions
  }): Promise<void> {
    await this.awaitConfig()
    const serializedOptions = options ? options.toJson() : new IdentityOptions().toJson()
    SuperwallExpoModule.identify(userId, serializedOptions)
  }

  /**
   * Resets the `userId`, on-device paywall assignments, and data stored by Superwall.
   *
   * @returns {Promise<void>} A promise that resolves once reset is complete.
   */
  async reset(): Promise<void> {
    await this.awaitConfig()
    await SuperwallExpoModule.reset()
  }

  /**
   * Handles a deep link.
   *
   * @param {string} url - The deep link to handle.
   * @returns {Promise<Boolean>} A promise that resolves to a boolean indicating whether the deep link was handled.
   */
  async handleDeepLink(url: string): Promise<boolean> {
    await this.awaitConfig()
    return await SuperwallExpoModule.handleDeepLink(url)
  }

  /**
   * Registers a placement to access a feature.
   *
   * When the placement is added to a campaign on the [Superwall Dashboard](https://superwall.com/dashboard),
   * it can trigger a paywall if the following conditions are met:
   * - The provided placement is included in a campaign on the Superwall Dashboard.
   * - The user matches an audience filter defined in the campaign.
   * - The user does not have an active subscription.
   *
   * Before using this method, ensure you have created a campaign and added the placement on the
   * [Superwall Dashboard](https://superwall.com/dashboard).
   *
   * The displayed paywall is determined by the audience filters set in the campaign.
   * Once a user is assigned a paywall within an audience, that paywall will continue to be shown unless
   * you remove it from the audience or reset the paywall assignments.
   *
   * @param {string} [params.placement] - The name of the placement to register.
   * @param {Map<string, any> | Record<string, any>} [params.params] - Optional parameters to pass with your placement.
   *   These parameters can be referenced within the audience filters of your campaign. Keys beginning with `$`
   *   are reserved for Superwall and will be omitted. Values can be any JSON-encodable value, URL, or Date.
   *   Arrays and dictionaries are not supported and will be dropped.
   * @param {PaywallPresentationHandler} [params.handler] - An optional handler that receives status updates
   *   about the paywall presentation.
   * @param {() => void} [params.feature] - An optional callback that will be executed after registration completes.
   *   If provided, this callback will be executed after the registration process completes successfully.
   *   If not provided, you can chain a `.then()` block to the returned promise to execute your feature logic.
   *
   * @returns {Promise<void>} if [feature] is provided this promise resolves when register is executed, otherwise a promise that resolves when register completes successfully after which you can chain a `.then()` block to execute your feature logic.
   *
   * @remarks
   * This behavior is remotely configurable via the [Superwall Dashboard](https://superwall.com/dashboard):
   *
   * - For _Non Gated_ paywalls, the feature block is executed when the paywall is dismissed or if the user is already paying.
   * - For _Gated_ paywalls, the feature block is executed only if the user is already paying or if they begin paying.
   * - If no paywall is configured, the feature block is executed immediately.
   * - If no feature block is provided, the returned promise will resolve when registration completes.
   * - If a feature block is provided, the returned promise will always resolve after the feature block is executed.
   * Note: The feature block will not be executed if an error occurs during registration. Such errors can be detected via the
   * `handler`.
   *
   * @example
   * // Using the feature callback:
   * Superwall.register({
   *   placement: "somePlacement",
   *   feature: () => {
   *     console.log("Feature logic executed after registration");
   *   }
   * });
   *
   * // Alternatively, chaining feature logic after registration:
   * Superwall.register({ placement: "somePlacement" })
   *   .then(() => {
   *     // Execute your feature logic here after registration.
   *     console.log("Placement registered, now executing feature logic.");
   *   })
   */
  async register(params: {
    placement: string
    params?: Map<string, any> | Record<string, any>
    handler?: PaywallPresentationHandler
    feature?: () => void
  }): Promise<void> {
    await this.awaitConfig()
    let handlerId: string | null = null

    if (params.handler) {
      const uuid = (+new Date() * Math.random()).toString(36)
      this.presentationHandlers.set(uuid, params.handler)
      handlerId = uuid
    }

    let paramsObject = {}
    if (params.params) {
      paramsObject =
        params.params instanceof Map ? Object.fromEntries(params.params) : params.params
    }

    if (params.feature) {
      return await SuperwallExpoModule.registerPlacement(
        params.placement,
        paramsObject,
        handlerId,
      ).then(() => {
        params.feature!() //TODO: This is wrong, the feature should be executed only if the native SDK calls the feature block
        // not after awaiting the promise
      })
    }

    return SuperwallExpoModule.registerPlacement(params.placement, paramsObject, handlerId)
  }

  /**
   * Confirms all experiment assignments and returns them in an array.
   *
   * Note that the assignments may differ when a placement is registered due to changes
   * in user, placement, or device parameters used in audience filters.
   *
   * @returns {Promise<Assignment[]>} A promise that resolves to an array of {@link Assignment} objects.
   */
  async confirmAllAssignments(): Promise<Assignment[]> {
    await this.awaitConfig()
    const assignments = await SuperwallExpoModule.confirmAllAssignments()
    return assignments.map((assignment: any) => Assignment.fromJson(assignment))
  }

  /**
   * Gets all the experiment assignments and returns them in an array.
   *
   * This method tracks the {@link SuperwallEvent.getAssignments} event in the delegate.
   *
   * Note that the assignments may differ when a placement is registered due to changes
   * in user, placement, or device parameters used in audience filters.
   *
   * @returns {Promise<Assignment[]>} A promise that resolves to an array of {@link Assignment} objects.
   */
  async getAssignments(): Promise<Assignment[]> {
    await this.awaitConfig()
    const assignments = await SuperwallExpoModule.getAssignments()
    return assignments.map((assignment: any) => Assignment.fromJson(assignment))
  }

  /**
   * Preemptively gets the result of registering a placement.
   *
   * This helps you determine whether a particular placement will present a paywall in the future.
   * Note that this method does not present a paywall. To present a paywall, use the `register` function.
   *
   * @param {Object} options - Options for obtaining the presentation result.
   * @param {string} options.placement - The name of the placement you want to register.
   * @param {Map<string, any>} [options.params] - Optional parameters to pass with your placement.
   *
   * @returns {Promise<PresentationResult>} A promise that resolves to a {@link PresentationResult} indicating the result of registering the placement.
   */
  async getPresentationResult({
    placement,
    params,
  }: {
    placement: string
    params?: Map<string, any>
  }): Promise<PresentationResult> {
    await this.awaitConfig()
    let paramsObject = {}
    if (params) {
      paramsObject = Object.fromEntries(params)
    }
    return await SuperwallExpoModule.getPresentationResult(placement, paramsObject)
  }

  /**
   * Retrieves the current configuration status of the Superwall SDK.
   *
   * This function returns a promise that resolves to the current configuration status,
   * indicating whether the SDK has finished configuring. Initially, the status is
   * {@link ConfigurationStatus.PENDING}. Once the configuration completes successfully, it
   * changes to {@link ConfigurationStatus.CONFIGURED}. If the configuration fails, the status
   * will be {@link ConfigurationStatus.FAILED}.
   *
   * @returns {Promise<ConfigurationStatus>} A promise that resolves with the current configuration status.
   */
  async getConfigurationStatus(): Promise<ConfigurationStatus> {
    const configurationStatusString = await SuperwallExpoModule.getConfigurationStatus()
    return ConfigurationStatus.fromString(configurationStatusString)
  }

  /**
   * Retrieves the entitlements tied to the device.
   *
   * @returns {Promise<EntitlementsInfo>} A promise that resolves to an {@link EntitlementsInfo} object.
   */
  async getEntitlements(): Promise<EntitlementsInfo> {
    await this.awaitConfig()
    const entitlementsJson = await SuperwallExpoModule.getEntitlements()
    return EntitlementsInfo.fromObject(entitlementsJson)
  }

  /**
   * Sets the subscription status of the user.
   *
   * When using a PurchaseController, you must call this method to update the user's subscription status.
   * Alternatively, you can implement the {@link SuperwallDelegate.subscriptionStatusDidChange} delegate callback to receive notifications
   * whenever the subscription status changes.
   *
   * @param {SubscriptionStatus} status - The new subscription status.
   *
   * @returns {Promise<void>} A promise that resolves once the subscription status has been updated.
   */
  async setSubscriptionStatus(status: SubscriptionStatus): Promise<void> {
    await this.awaitConfig()
    await SuperwallExpoModule.setSubscriptionStatus(status)
  }

  async getSubscriptionStatus(): Promise<SubscriptionStatus> {
    await this.awaitConfig()
    const subscriptionStatusData = await SuperwallExpoModule.getSubscriptionStatus()
    return SubscriptionStatus.fromJson(subscriptionStatusData.subscriptionStatus)
  }
  /**
   * Sets the user interface style, which overrides the system setting.
   *
   * Provide a value of type {@link InterfaceStyle} to explicitly set the interface style.
   * Pass `null` to revert back to the system's default interface style.
   *
   * @param {InterfaceStyle | null} style - The desired interface style, or `null` to use the system setting.
   *
   * @returns {Promise<void>} A promise that resolves once the interface style has been updated.
   */
  async setInterfaceStyle(style: InterfaceStyle | null): Promise<void> {
    await SuperwallExpoModule.setInterfaceStyle(style?.toString())
  }

  /**
   * Sets the delegate that handles Superwall lifecycle events.
   *
   * @param {SuperwallDelegate | undefined} delegate - An object implementing the {@link SuperwallDelegate} interface,
   * or `undefined` to remove the current delegate.
   *
   * @returns {Promise<void>} A promise that resolves once the delegate has been updated.
   */
  async setDelegate(delegate: SuperwallDelegate | undefined): Promise<void> {
    await this.awaitConfig()
    Superwall.delegate = delegate
  }

  /**
   * Retrieves the user attributes, set using {@link setUserAttributes}.
   *
   * @returns {Promise<UserAttributes>} A promise that resolves with an object representing the user's attributes.
   */
  async getUserAttributes(): Promise<UserAttributes> {
    await this.awaitConfig()
    const userAttributes: UserAttributes = await SuperwallExpoModule.getUserAttributes()
    return userAttributes
  }

  /**
   * Preloads all paywalls that the user may see based on campaigns and placements in your Superwall dashboard.
   *
   * To use this, first set `PaywallOptions.shouldPreload` to `false` when configuring the SDK.
   * Then call this function when you want preloading to begin.
   *
   * Note: This method will not reload any paywalls that have already been preloaded via {@link preloadPaywalls}.
   *
   * @returns {Promise<void>} A promise that resolves once the preloading process has been initiated.
   */
  async preloadAllPaywalls(): Promise<void> {
    await this.awaitConfig()
    await SuperwallExpoModule.preloadAllPaywalls()
  }

  /**
   * Preloads paywalls for specific placements.
   *
   * To use this method, first ensure that {@link PaywallOptions.shouldPreload} is set to `false` when configuring the SDK.
   * Then call this function when you want to initiate preloading for selected placements.
   *
   * Note: This will not reload any paywalls you've already preloaded.
   *
   * @param {Set<string>} placementNames - A set of placement names whose paywalls you want to preload.
   *
   * @returns {Promise<void>} A promise that resolves once the preloading process has been initiated.
   */
  async preloadPaywalls(placementNames: Set<string>): Promise<void> {
    await this.awaitConfig()
    await SuperwallExpoModule.preloadPaywalls(Array.from(placementNames))
  }

  /**
   * Sets user attributes for use in paywalls and on the Superwall dashboard.
   *
   * If an attribute already exists, its value will be overwritten while other attributes remain unchanged.
   * This is useful for analytics and campaign audience filters you may define in the Superwall Dashboard.
   *
   * **Note:** These attributes should not be used as a source of truth for sensitive information.
   *
   * For example, after retrieving your user's data:
   *
   * ```ts
   * const attributes: UserAttributes = {
   *   name: user.name,
   *   apnsToken: user.apnsTokenString,
   *   email: user.email,
   *   username: user.username,
   *   profilePic: user.profilePicUrl,
   * }
   * await Superwall.setUserAttributes(attributes)
   * ```
   *
   * See [Setting User Attributes](https://docs.superwall.com/docs/setting-user-properties) for more information.
   *
   * @param {UserAttributes} userAttributes - An object containing custom attributes to store for the user.
   *   Values can be any JSON-encodable value, URLs, or Dates. Keys beginning with `$` are reserved for Superwall and will be dropped.
   *   Arrays and dictionaries as values are not supported and will be omitted.
   *
   * @returns {Promise<void>} A promise that resolves once the user attributes have been updated.
   */
  async setUserAttributes(userAttributes: UserAttributes): Promise<void> {
    await this.awaitConfig()
    SuperwallExpoModule.setUserAttributes(userAttributes)
  }

  /**
   * Dismisses the presented paywall, if one exists.
   *
   * @returns {Promise<void>} A promise that resolves once the paywall has been dismissed,
   * or immediately if no paywall was active.
   */
  async dismiss(): Promise<void> {
    await SuperwallExpoModule.dismiss()
  }

  async setLogLevel(level: LogLevel): Promise<void> {
    await SuperwallExpoModule.setLogLevel(level.toString())
  }
}
