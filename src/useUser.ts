import { useSuperwall } from "./useSuperwall"

export const useUser = () => {
  const { identify, update, user, signOut } = useSuperwall((state) => ({
    identify: state.identify,
    user: state.user,
    update: state.setUserAttributes,
    signOut: state.reset,
  }))

  return {
    identify,
    update,
    signOut: signOut,
    user,
  } as const
}
