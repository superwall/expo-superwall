import { create } from "zustand"
import { subscribeWithSelector } from "zustand/middleware"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type {
  LogLevel,
  PaywallInfo,
  PaywallResult,
  PaywallSkippedReason,
  SubscriptionStatus,
} from "./SuperwallExpoModule.types"

import type { EventSubscription } from "expo-modules-core"

interface SuperwallState {
  // Configuration
  isConfigured: boolean
  configurationStatus: string | null
  error: string | null

  // User state
  userId: string | null
  userAttributes: Record<string, any>
  subscriptionStatus: SubscriptionStatus
  entitlements: any[]
  assignments: any[]

  // Paywall state
  paywalls: Record<
    string,
    {
      status: "idle" | "loading" | "presented" | "dismissed" | "skipped" | "error"
      paywallInfo?: PaywallInfo
      result?: PaywallResult
      reason?: PaywallSkippedReason
      error?: string
    }
  >

  // Internal
  subscriptions: EventSubscription[]
  eventHandlers: Record<string, any>
}

interface SuperwallActions {
  // Configuration
  configure: (
    apiKey: string,
    options?: Record<string, any>,
    usingPurchaseController?: boolean,
    sdkVersion?: string,
    logLevel?: LogLevel,
  ) => Promise<void>

  // Internal paywall listeners
  _setupPaywallListeners: (handlerId: string, handlers: any) => void

  // User actions
  identify: (userId: string, options?: Record<string, any>) => Promise<void>
  reset: () => Promise<void>
  updateUserAttributes: (attributes: Record<string, any>) => Promise<void>
  updateSubscriptionStatus: (status: SubscriptionStatus) => Promise<void>
  refreshUserData: () => Promise<void>

  // Paywall actions
  registerPlacement: (
    placement: string,
    params?: Record<string, any>,
    handlers?: any,
  ) => Promise<string>

  // Event management
  addEventHandlers: (id: string, handlers: any) => () => void

  // Utility
  handleDeepLink: (url: string) => Promise<boolean>
  setLogLevel: (level: LogLevel) => void
  preloadPaywalls: (placementNames: string[]) => void
  preloadAllPaywalls: () => void
  confirmAllAssignments: () => Promise<any[]>
  dismiss: () => Promise<void>
  didPurchase: (result: Record<string, any>) => Promise<void>
  didRestore: (result: Record<string, any>) => Promise<void>

  // Internal
  _setupCoreEventListeners: () => void
  _setupEventListeners: (id: string, handlers?: any) => void
  _handleError: (message: string, error: unknown) => void
  destroy: () => void
}

type SuperwallStore = SuperwallState & SuperwallActions

export const useSuperwallStore = create<SuperwallStore>()(
  subscribeWithSelector((set, get) => ({
    // Initial state
    isConfigured: false,
    configurationStatus: null,
    error: null,
    userId: null,
    userAttributes: {},
    subscriptionStatus: { status: "UNKNOWN" },
    entitlements: [],
    assignments: [],
    paywalls: {},
    subscriptions: [],
    eventHandlers: {},

    // Actions
    async configure(apiKey, options, usingPurchaseController, sdkVersion, logLevel = "warn") {
      const state = get()
      if (state.isConfigured) return

      try {
        await SuperwallExpoModule.configure(apiKey, options, usingPurchaseController, sdkVersion)
        SuperwallExpoModule.setLogLevel(logLevel)

        const status = await SuperwallExpoModule.getConfigurationStatus()

        // Load initial data
        const [attrs, subStatus, entitlements, assignments] = await Promise.all([
          SuperwallExpoModule.getUserAttributes(),
          SuperwallExpoModule.getSubscriptionStatus(),
          SuperwallExpoModule.getEntitlements(),
          SuperwallExpoModule.getAssignments(),
        ])

        set({
          isConfigured: true,
          configurationStatus: status,
          userAttributes: attrs,
          subscriptionStatus: subStatus,
          entitlements,
          assignments,
          error: null,
        })

        get()._setupCoreEventListeners()
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error)
        set({ error: errorMessage })
        throw error
      }
    },

    _setupCoreEventListeners() {
      const subscription = SuperwallExpoModule.addListener(
        "subscriptionStatusDidChange",
        (params) => {
          set({ subscriptionStatus: params.to })

          // Notify event handlers
          const { eventHandlers } = get()
          Object.values(eventHandlers).forEach((handlers: any) => {
            handlers.onSubscriptionStatusChange?.(params.from, params.to)
          })
        },
      )

      set((state) => ({
        subscriptions: [...state.subscriptions, subscription],
      }))
    },

    addEventHandlers(id, handlers) {
      set((state) => ({
        eventHandlers: { ...state.eventHandlers, [id]: handlers },
      }))

      get()._setupEventListeners(id, handlers)

      return () => {
        set((state) => {
          const { [id]: removed, ...rest } = state.eventHandlers
          return { eventHandlers: rest }
        })
      }
    },

    _setupEventListeners(id, handlers) {
      const subscriptions: EventSubscription[] = []

      if (handlers.onEvent) {
        subscriptions.push(
          SuperwallExpoModule.addListener("handleSuperwallEvent", (params) => {
            handlers.onEvent(params.eventInfo)
          }),
        )
      }

      if (handlers.onLog) {
        subscriptions.push(SuperwallExpoModule.addListener("handleLog", handlers.onLog))
      }

      if (handlers.onCustomPaywallAction) {
        subscriptions.push(
          SuperwallExpoModule.addListener("handleCustomPaywallAction", (params) => {
            handlers.onCustomPaywallAction(params.name)
          }),
        )
      }

      if (handlers.onPurchase) {
        subscriptions.push(SuperwallExpoModule.addListener("onPurchase", handlers.onPurchase))
      }

      if (handlers.onRestore) {
        subscriptions.push(SuperwallExpoModule.addListener("onPurchaseRestore", handlers.onRestore))
      }

      set((state) => ({
        subscriptions: [...state.subscriptions, ...subscriptions],
      }))
    },

    async identify(userId, options) {
      const state = get()
      if (!state.isConfigured) {
        throw new Error("Superwall is not configured")
      }

      try {
        SuperwallExpoModule.identify(userId, options)
        await get().refreshUserData()
        set({ userId })
      } catch (error) {
        get()._handleError("Failed to identify user", error)
      }
    },

    async reset() {
      const state = get()
      if (!state.isConfigured) {
        throw new Error("Superwall is not configured")
      }

      try {
        SuperwallExpoModule.reset()
        set({
          userId: null,
          userAttributes: {},
          subscriptionStatus: { status: "UNKNOWN" },
          entitlements: [],
          assignments: [],
        })
      } catch (error) {
        get()._handleError("Failed to reset user", error)
      }
    },

    async updateUserAttributes(attributes) {
      const state = get()
      if (!state.isConfigured) {
        throw new Error("Superwall is not configured")
      }

      try {
        SuperwallExpoModule.setUserAttributes(attributes)
        await get().refreshUserData()
      } catch (error) {
        get()._handleError("Failed to update user attributes", error)
      }
    },

    async updateSubscriptionStatus(status) {
      const state = get()
      if (!state.isConfigured) {
        throw new Error("Superwall is not configured")
      }

      try {
        SuperwallExpoModule.setSubscriptionStatus(status)
        set({ subscriptionStatus: status })
      } catch (error) {
        get()._handleError("Failed to update subscription status", error)
      }
    },

    async refreshUserData() {
      try {
        const [attrs, subStatus, entitlements, assignments] = await Promise.all([
          SuperwallExpoModule.getUserAttributes(),
          SuperwallExpoModule.getSubscriptionStatus(),
          SuperwallExpoModule.getEntitlements(),
          SuperwallExpoModule.getAssignments(),
        ])

        set({
          userAttributes: attrs,
          subscriptionStatus: subStatus,
          entitlements,
          assignments,
        })
      } catch (error) {
        console.error("Failed to refresh user data:", error)
      }
    },

    async registerPlacement(placement, params, handlers) {
      const state = get()
      if (!state.isConfigured) {
        throw new Error("Superwall is not configured")
      }

      const handlerId = `${Date.now()}-${Math.random()}`

      // Set loading state
      set((state) => ({
        paywalls: {
          ...state.paywalls,
          [handlerId]: { status: "loading" },
        },
      }))

      try {
        if (handlers) {
          get()._setupPaywallListeners(handlerId, handlers)
        }

        await SuperwallExpoModule.registerPlacement(placement, params, handlerId)
        return handlerId
      } catch (error) {
        set((state) => ({
          paywalls: {
            ...state.paywalls,
            [handlerId]: {
              status: "error",
              error: error instanceof Error ? error.message : String(error),
            },
          },
        }))
        throw error
      }
    },

    _setupPaywallListeners(handlerId, handlers) {
      const subscriptions: EventSubscription[] = []

      if (handlers.onPresent) {
        subscriptions.push(
          SuperwallExpoModule.addListener("onPaywallPresent", (params) => {
            if (params.handlerId === handlerId) {
              set((state) => ({
                paywalls: {
                  ...state.paywalls,
                  [handlerId]: {
                    status: "presented",
                    paywallInfo: params.paywallInfoJson,
                  },
                },
              }))
              handlers.onPresent(params.paywallInfoJson)
            }
          }),
        )
      }

      if (handlers.onDismiss) {
        subscriptions.push(
          SuperwallExpoModule.addListener("onPaywallDismiss", (params) => {
            if (params.handlerId === handlerId) {
              set((state) => ({
                paywalls: {
                  ...state.paywalls,
                  [handlerId]: {
                    status: "dismissed",
                    paywallInfo: params.paywallInfoJson,
                    result: params.result,
                  },
                },
              }))
              handlers.onDismiss(params.paywallInfoJson, params.result)

              setTimeout(() => {
                set((state) => {
                  const { [handlerId]: removed, ...rest } = state.paywalls
                  return { paywalls: rest }
                })
              }, 1000)
            }
          }),
        )
      }

      if (handlers.onError) {
        subscriptions.push(
          SuperwallExpoModule.addListener("onPaywallError", (params) => {
            if (params.handlerId === handlerId) {
              set((state) => ({
                paywalls: {
                  ...state.paywalls,
                  [handlerId]: {
                    status: "error",
                    error: params.errorString,
                  },
                },
              }))
              handlers.onError(params.errorString)
            }
          }),
        )
      }

      if (handlers.onSkip) {
        subscriptions.push(
          SuperwallExpoModule.addListener("onPaywallSkip", (params) => {
            if (params.handlerId === handlerId) {
              set((state) => ({
                paywalls: {
                  ...state.paywalls,
                  [handlerId]: {
                    status: "skipped",
                    reason: params.skippedReason,
                  },
                },
              }))
              handlers.onSkip(params.skippedReason)
            }
          }),
        )
      }

      set((state) => ({
        subscriptions: [...state.subscriptions, ...subscriptions],
      }))
    },

    async handleDeepLink(url) {
      const state = get()
      if (!state.isConfigured) {
        console.warn("Superwall is not configured")
        return false
      }

      try {
        return await SuperwallExpoModule.handleDeepLink(url)
      } catch (error) {
        get()._handleError("Failed to handle deep link", error)
        return false
      }
    },

    setLogLevel(level) {
      SuperwallExpoModule.setLogLevel(level)
    },

    preloadPaywalls(placementNames) {
      const state = get()
      if (!state.isConfigured) {
        console.warn("Superwall is not configured")
        return
      }
      SuperwallExpoModule.preloadPaywalls(placementNames)
    },

    preloadAllPaywalls() {
      const state = get()
      if (!state.isConfigured) {
        console.warn("Superwall is not configured")
        return
      }
      SuperwallExpoModule.preloadAllPaywalls()
    },

    async confirmAllAssignments() {
      const state = get()
      if (!state.isConfigured) {
        console.warn("Superwall is not configured")
        return []
      }

      try {
        return await SuperwallExpoModule.confirmAllAssignments()
      } catch (error) {
        get()._handleError("Failed to confirm assignments", error)
        return []
      }
    },

    async dismiss() {
      const state = get()
      if (!state.isConfigured) {
        console.warn("Superwall is not configured")
        return
      }

      try {
        await SuperwallExpoModule.dismiss()
      } catch (error) {
        get()._handleError("Failed to dismiss paywall", error)
      }
    },

    async didPurchase(result) {
      const state = get()
      if (!state.isConfigured) {
        console.warn("Superwall is not configured")
        return
      }

      try {
        SuperwallExpoModule.didPurchase(result)
        await get().refreshUserData()
      } catch (error) {
        get()._handleError("Failed to report purchase", error)
      }
    },

    async didRestore(result) {
      const state = get()
      if (!state.isConfigured) {
        console.warn("Superwall is not configured")
        return
      }

      try {
        SuperwallExpoModule.didRestore(result)
        await get().refreshUserData()
      } catch (error) {
        get()._handleError("Failed to report restore", error)
      }
    },

    _handleError(message, error) {
      const errorMessage = error instanceof Error ? error.message : String(error)
      console.error(`${message}:`, errorMessage)

      // Notify error handlers
      const { eventHandlers } = get()
      Object.values(eventHandlers).forEach((handlers: any) => {
        handlers.onError?.(errorMessage)
      })
    },

    destroy() {
      const { subscriptions } = get()
      subscriptions.forEach((subscription) => subscription.remove())
      set({
        subscriptions: [],
        eventHandlers: {},
      })
    },
  })),
)
