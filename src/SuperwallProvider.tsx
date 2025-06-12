import { type ReactNode, useEffect } from "react"
import { Platform } from "react-native"
import { useShallow } from "zustand/shallow"
import { SuperwallContext, useSuperwallStore } from "./useSuperwall"

interface SuperwallProviderProps {
  /** Your Superwall API key */
  apiKeys: {
    android?: string
    ios?: string
  }
  /** Optional configuration options passed to the native SDK */
  options?: Record<string, any>
  /** App content to render once configured */
  children: ReactNode
}

/**
 * Wrap your application in `SuperwallProvider` to initialise the Superwall SDK.
 *
 * Example:
 * ```tsx
 * <SuperwallProvider apiKey="YOUR_API_KEY">
 *   <App />
 * </SuperwallProvider>
 * ```
 */
export function SuperwallProvider({
  apiKeys,
  options,

  children,
}: SuperwallProviderProps) {
  const { isConfigured, isLoading, configure } = useSuperwallStore(
    useShallow((state) => ({
      isConfigured: state.isConfigured,
      isLoading: state.isLoading,
      configure: state.configure,
    })),
  )

  useEffect(() => {
    if (!isConfigured && !isLoading) {
      const apiKey = apiKeys[Platform.OS as keyof typeof apiKeys]
      if (!apiKey) {
        throw new Error(`No API key provided for platform ${Platform.OS}`)
      }

      configure(apiKey, options).catch((err) => {
        console.error("Superwall configure failed", err)
      })
    }
  }, [isConfigured, isLoading, apiKeys, options, configure])

  useEffect(() => {
    const cleanup = useSuperwallStore.getState()._initListeners()

    return cleanup
  }, [])

  return <SuperwallContext.Provider value={true}>{children}</SuperwallContext.Provider>
}
