import { useSuperwall } from "./useSuperwall"

export const useUser = () => {
  const { identify, update, user, signOut, refresh } = useSuperwall((state) => ({
    identify: state.identify,
    user: state.user,
    update: state.setUserAttributes,
    signOut: state.reset,
    refresh: state.getUserAttributes,
  }))

  return {
    identify,
    update,
    signOut,
    refresh,
    user,
  } as const
}
