import { useEffect } from "react"
import { useSuperwallStore } from "./store"

export interface UseSuperwallOptions {
  onEvent?: (eventInfo: any) => void
  onError?: (error: string) => void
}

export const useSuperwall = (options?: UseSuperwallOptions) => {
  const store = useSuperwallStore()

  useEffect(() => {
    if (options) {
      const cleanup = store.addEventHandlers("useSuperwall", options)
      return cleanup
    }
  }, [store, options])

  return {
    isConfigured: store.isConfigured,
    configurationStatus: store.configurationStatus,
    error: store.error,
    subscriptionStatus: store.subscriptionStatus,
    userAttributes: store.userAttributes,
    userId: store.userId,
    entitlements: store.entitlements,
    assignments: store.assignments,
    handleDeepLink: store.handleDeepLink,
    setLogLevel: store.setLogLevel,
    preloadPaywalls: store.preloadPaywalls,
    preloadAllPaywalls: store.preloadAllPaywalls,
    confirmAllAssignments: store.confirmAllAssignments,
    dismiss: store.dismiss,
  }
}
