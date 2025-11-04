import { useSuperwall } from "./useSuperwall"

/**
 * @category Hooks
 * @since 0.0.15
 * React hook for managing user-specific aspects of the Superwall SDK.
 *
 * This hook provides functions to identify the user, update user attributes,
 * sign out the user, and refresh user data. It also provides access to the
 * current user's attributes and subscription status.
 *
 * It must be used within a component that is a descendant of `<SuperwallProvider />`.
 *
 * @returns An object containing user-related state and functions.
 *   - `identify: (userId: string, options?: IdentifyOptions) => Promise<void>`:
 *     Identifies the current user with Superwall. This is typically called when a user logs in.
 *     - `userId`: The unique identifier for the user.
 *     - `options`: Optional parameters for identification, such as `restorePaywallAssignments`.
 *   - `update: (attributes: Record<string, any> | ((old: Record<string, any>) => Record<string, any>)) => Promise<void>`:
 *     Updates the current user's attributes. Accepts either an object of attributes or a function
 *     that takes the old attributes and returns the new attributes.
 *   - `signOut: () => void`: Resets the user's identity, effectively signing them out from Superwall.
 *     This clears all user-specific data from the SDK.
 *   - `refresh: () => Promise<Record<string, any>>`:
 *     Manually refreshes the user's attributes and subscription status from the Superwall servers.
 *     Returns a promise that resolves with the refreshed user attributes.
 *   - `setIntegrationAttributes: (attributes: IntegrationAttributes) => Promise<void>`:
 *     Sets attributes for third-party integrations (e.g., attribution providers, analytics platforms).
 *     Used to link user IDs from services like Adjust, Amplitude, AppsFlyer, etc.
 *   - `getIntegrationAttributes: () => Promise<Record<string, string>>`:
 *     Gets the currently set integration attributes.
 *   - `subscriptionStatus?: SubscriptionStatus`: The current subscription status of the user.
 *     (See `SubscriptionStatus` type in `SuperwallExpoModule.types.ts`).
 *   - `user?: UserAttributes | null`: An object containing the current user's attributes
 *     (e.g., `appUserId`, `aliasId`, custom attributes). `null` if no user is identified or after `signOut`.
 *     (See `UserAttributes` type in `useSuperwall.ts`).
 *
 * @example
 * const { identify, update, signOut, user, subscriptionStatus } = useUser();
 *
 * const handleLogin = async (userId: string) => {
 *   await identify(userId);
 * };
 *
 * const handleUpdateName = async (newName: string) => {
 *   await update({ name: newName });
 * };
 *
 * const handleLogout = () => {
 *   signOut();
 * };
 */
export const useUser = () => {
  const {
    identify,
    internalUpdate,
    user,
    signOut,
    refresh,
    subscriptionStatus,
    setSubscriptionStatus,
    setIntegrationAttributes,
    getIntegrationAttributes,
  } = useSuperwall((state) => ({
    identify: state.identify,
    user: state.user,
    internalUpdate: state.setUserAttributes,
    signOut: state.reset,
    refresh: state.getUserAttributes,
    subscriptionStatus: state.subscriptionStatus,
    setSubscriptionStatus: state.setSubscriptionStatus,
    setIntegrationAttributes: state.setIntegrationAttributes,
    getIntegrationAttributes: state.getIntegrationAttributes,
  }))

  const update = async (
    attributes: Record<string, any> | ((old: Record<string, any>) => Record<string, any>),
  ) => {
    if (typeof attributes === "function") {
      const stuff = attributes(user || {})
      await internalUpdate(stuff)
      return
    }

    await internalUpdate(attributes)
  }

  return {
    identify,
    update,
    signOut,
    refresh,
    subscriptionStatus,
    setSubscriptionStatus,
    setIntegrationAttributes,
    getIntegrationAttributes,
    user,
  } as const
}
