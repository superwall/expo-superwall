import { useSuperwall } from "../useSuperwall"

export const SuperwallLoading = ({ children }: { children: React.ReactNode }) => {
  const isLoaded = useSuperwall((state) => !state.isLoading && state.isConfigured)

  if (isLoaded) {
    return null
  }

  return children
}

export const SuperwallLoaded = ({ children }: { children: React.ReactNode }) => {
  const isLoaded = useSuperwall((state) => !state.isLoading && state.isConfigured)

  if (!isLoaded) {
    return null
  }

  return children
}
