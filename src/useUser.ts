import { useSuperwall } from "./useSuperwall"

export const useUser = () => {
  const { identify, internalUpdate, user, signOut, refresh, subscriptionStatus } = useSuperwall(
    (state) => ({
      identify: state.identify,
      user: state.user,
      internalUpdate: state.setUserAttributes,
      signOut: state.reset,
      refresh: state.getUserAttributes,
      subscriptionStatus: state.subscriptionStatus,
    }),
  )

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
    user,
  } as const
}
