import { createContext, useContext } from "react"
import { create } from "zustand"
import { useShallow } from "zustand/shallow"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type {
  PaywallInfo,
  PaywallResult,
  PaywallSkippedReason,
  SubscriptionStatus,
} from "./SuperwallExpoModule.types"

export interface UserAttributes {
  aliasId: string
  appUserId: string
  applicationInstalledAt: string
  seed: number
  [key: string]: any
}

export interface IdentifyOptions {
  restorePaywallAssignments?: boolean
}

// Minimal shape of the store representing the key pieces of state and actions
export interface SuperwallStore {
  /* -------------------- State -------------------- */
  isConfigured: boolean
  isLoading: boolean

  user?: UserAttributes | null

  subscriptionStatus?: SubscriptionStatus
  activePaywallInfo?: PaywallInfo
  lastPaywallResult?: PaywallResult
  lastSkippedReason?: PaywallSkippedReason
  lastError?: string | null

  /* -------------------- Internal -------------------- */
  // Internal listener references for cleanup handled inside Provider effect.
  // Not reactive, so we store outside Zustand state to avoid unnecessary rerenders.

  /* -------------------- Actions -------------------- */
  // Initialisation & identity
  configure: (
    apiKey: string,
    options?: Record<string, any>,
    usingPurchaseController?: boolean,
    sdkVersion?: string,
  ) => Promise<void>
  identify: (userId: string, options?: IdentifyOptions) => Promise<void>
  reset: () => void

  // Paywall / placements
  registerPlacement: (
    placement: string,
    params?: Record<string, any>,
    handlerId?: string | null,
  ) => Promise<void>
  getPresentationResult: (placement: string, params?: Record<string, any>) => Promise<any>
  dismiss: () => Promise<void>

  // Preloading
  preloadAllPaywalls: () => Promise<void>
  preloadPaywalls: (placements: string[]) => Promise<void>

  // Attributes
  setUserAttributes: (attrs: Record<string, any>) => Promise<void>
  getUserAttributes: () => Promise<Record<string, any>>

  // Logging & misc
  setLogLevel: (level: string) => Promise<void>

  /* -------------------- Listener helpers -------------------- */
  _initListeners: () => (() => void) | undefined
}

export const useSuperwallStore = create<SuperwallStore>((set, get) => ({
  /* -------------------- State -------------------- */
  isConfigured: false,
  isLoading: false,

  user: null,
  subscriptionStatus: undefined,
  activePaywallInfo: undefined,
  lastPaywallResult: undefined,
  lastSkippedReason: undefined,
  lastError: null,

  /* -------------------- Actions -------------------- */
  configure: async (apiKey, options, usingPurchaseController, sdkVersion) => {
    set({ isLoading: true })
    await SuperwallExpoModule.configure(apiKey, options, usingPurchaseController, sdkVersion)
    set({ isConfigured: true, isLoading: false })

    const currentUser = (await SuperwallExpoModule.getUserAttributes()) as UserAttributes
    set({ user: currentUser })
  },
  identify: async (userId, options) => {
    SuperwallExpoModule.identify(userId, options)

    // TODO: Instead of setting users after identify, we should set this based on an event
    const currentUser = (await SuperwallExpoModule.getUserAttributes()) as UserAttributes
    set({ user: currentUser })
  },
  reset: () => {
    SuperwallExpoModule.reset()

    set({ user: null })
  },
  registerPlacement: async (placement, params, handlerId) => {
    await SuperwallExpoModule.registerPlacement(placement, params, handlerId)
  },
  getPresentationResult: async (placement, params) => {
    return SuperwallExpoModule.getPresentationResult(placement, params)
  },
  dismiss: async () => {
    await SuperwallExpoModule.dismiss()
  },
  preloadAllPaywalls: async () => {
    SuperwallExpoModule.preloadAllPaywalls()
  },
  preloadPaywalls: async (placements) => {
    SuperwallExpoModule.preloadPaywalls(placements)
  },
  setUserAttributes: async (attrs) => {
    SuperwallExpoModule.setUserAttributes(attrs)

    const currentUser = (await SuperwallExpoModule.getUserAttributes()) as UserAttributes
    set({ user: currentUser })
  },
  getUserAttributes: async () => {
    return SuperwallExpoModule.getUserAttributes()
  },
  setLogLevel: async (level) => {
    SuperwallExpoModule.setLogLevel(level)
  },

  /* -------------------- Listener helpers -------------------- */
  _initListeners: (): (() => void) | undefined => {
    if (listenersInitialised) return storedCleanup

    const subscriptions: { remove: () => void }[] = []

    // Subscription Status updates
    subscriptions.push(
      SuperwallExpoModule.addListener(
        "subscriptionStatusDidChange",
        ({ to }: { to: SubscriptionStatus }) => {
          set({ subscriptionStatus: to })
        },
      ),
    )

    // Paywall presented
    subscriptions.push(
      SuperwallExpoModule.addListener(
        "onPaywallPresent",
        ({ paywallInfoJson }: { paywallInfoJson: PaywallInfo }) => {
          set({ activePaywallInfo: paywallInfoJson })
        },
      ),
    )

    // Paywall dismissed
    subscriptions.push(
      SuperwallExpoModule.addListener(
        "onPaywallDismiss",
        ({ paywallInfoJson, result }: { paywallInfoJson: PaywallInfo; result: PaywallResult }) => {
          set({ activePaywallInfo: undefined, lastPaywallResult: result })
        },
      ),
    )

    // Paywall skipped
    subscriptions.push(
      SuperwallExpoModule.addListener(
        "onPaywallSkip",
        ({ skippedReason }: { skippedReason: PaywallSkippedReason }) => {
          set({ lastSkippedReason: skippedReason })
        },
      ),
    )

    // Paywall error
    subscriptions.push(
      SuperwallExpoModule.addListener(
        "onPaywallError",
        ({ errorString }: { errorString: string }) => {
          set({ lastError: errorString })
        },
      ),
    )

    listenersInitialised = true
    storedSubscriptions = subscriptions

    // Define cleanup
    storedCleanup = (): void => {
      storedSubscriptions.forEach((s) => s.remove())
      listenersInitialised = false
      storedSubscriptions = []
    }

    return storedCleanup
  },
}))

// Module-level variables to track listener state without triggering store updates
let listenersInitialised = false
let storedSubscriptions: { remove: () => void }[] = []
let storedCleanup: (() => void) | undefined

// Context to ensure components are wrapped in <SuperwallProvider />
export const SuperwallContext = createContext<boolean>(false)

/**
 * React hook that exposes the Superwall store.
 * Automatically ensures native event listeners are set up on first use.
 * If `selector` is omitted, the entire store is returned.
 */
export function useSuperwall<T = SuperwallStore>(selector?: (state: SuperwallStore) => T): T {
  const inProvider = useContext(SuperwallContext)
  if (!inProvider) {
    throw new Error("useSuperwall must be used within a SuperwallProvider")
  }

  const identity = (state: SuperwallStore) => state as unknown as T
  // biome-ignore lint/correctness/useHookAtTopLevel: <explanation>
  return useSuperwallStore(selector ? useShallow(selector) : identity)
}
