import { useSuperwall } from "./useSuperwall"

export const useUser = () => {
  const { identify, internalUpdate, user, signOut, refresh } = useSuperwall((state) => ({
    identify: state.identify,
    user: state.user,
    internalUpdate: state.setUserAttributes,
    signOut: state.reset,
    refresh: state.getUserAttributes,
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
    user,
  } as const
}
