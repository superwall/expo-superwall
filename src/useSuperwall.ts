import { createContext, useContext } from "react"
import { create } from "zustand"
import { useShallow } from "zustand/shallow"
import pkg from "../package.json"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type { SubscriptionStatus } from "./SuperwallExpoModule.types"
import type { SuperwallOptions } from "./SuperwallOptions"

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
  [key: string]: any
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
   * The current user's attributes.
   * `null` if no user is identified or after `reset` is called.
   * `undefined` initially before any user data is fetched or set.
   */
  user?: UserAttributes | null

  /** The current subscription status of the user. */
  subscriptionStatus: SubscriptionStatus

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
    options?: Partial<SuperwallOptions> & {
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
  getPresentationResult: (placement: string, params?: Record<string, any>) => Promise<any>
  /**
   * Dismisses any currently presented Superwall paywall.
   * @returns A promise that resolves when the dismissal is complete.
   */
  dismiss: () => Promise<void>

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
  setUserAttributes: (attrs: Record<string, any>) => Promise<void>
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

  user: null,
  subscriptionStatus: {
    status: "UNKNOWN",
  },

  /* -------------------- Actions -------------------- */
  configure: async (apiKey, options) => {
    set({ isLoading: true })
    const { manualPurchaseManagement, manualPurchaseManagment, ...restOptions } = options || {}

    // Support both spellings for backward compatibility
    const isManualPurchaseManagement = manualPurchaseManagement ?? manualPurchaseManagment ?? false

    await SuperwallExpoModule.configure(apiKey, restOptions, isManualPurchaseManagement, pkg.version)

    const currentUser = await SuperwallExpoModule.getUserAttributes()
    const subscriptionStatus = await SuperwallExpoModule.getSubscriptionStatus()

    set({
      isConfigured: true,
      isLoading: false,
      user: currentUser as UserAttributes,
      subscriptionStatus,
    })
  },
  identify: async (userId, options) => {
    await SuperwallExpoModule.identify(userId, options)

    // TODO: Instead of setting users after identify, we should set this based on an event
    setTimeout(async () => {
      const currentUser = await SuperwallExpoModule.getUserAttributes()
      const subscriptionStatus = await SuperwallExpoModule.getSubscriptionStatus()
      set({ user: currentUser as UserAttributes, subscriptionStatus })
    }, 0)
  },
  reset: async () => {
    await SuperwallExpoModule.reset()

    const currentUser = await SuperwallExpoModule.getUserAttributes()
    const subscriptionStatus = await SuperwallExpoModule.getSubscriptionStatus()

    set({ user: currentUser as UserAttributes, subscriptionStatus })
  },
  registerPlacement: async (placement, params, handlerId = "default") => {
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
    await SuperwallExpoModule.setUserAttributes(attrs)

    const currentUser = await SuperwallExpoModule.getUserAttributes()
    set({ user: currentUser as UserAttributes })
  },
  getUserAttributes: async () => {
    const attributes = await SuperwallExpoModule.getUserAttributes()
    set({ user: attributes as UserAttributes })
    return attributes
  },
  setLogLevel: async (level) => {
    SuperwallExpoModule.setLogLevel(level)
  },

  setSubscriptionStatus: async (status) => {
    await SuperwallExpoModule.setSubscriptionStatus(status)
  },
  getDeviceAttributes: async () => {
    const attributes = await SuperwallExpoModule.getDeviceAttributes()
    return attributes
  },

  /* -------------------- Listener helpers -------------------- */
  _initListeners: (): (() => void) => {
    // Use get() to read the state from within the store
    if (get().listenersInitialized) {
      console.warn("Listeners already initialized. Skipping.")
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

    set({ listenersInitialized: true })
    console.log("Initialized listeners", subscriptions.length)

    return (): void => {
      console.log("Cleaning up listeners", subscriptions.length)
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
