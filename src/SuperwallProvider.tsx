import { type ReactNode, useEffect } from "react"
import { useShallow } from "zustand/shallow"
import { SuperwallContext, useSuperwallStore } from "./useSuperwall"

interface SuperwallProviderProps {
  /** Your Superwall API key */
  apiKey: string
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
  apiKey,
  options,

  children,
}: SuperwallProviderProps) {
  const { isConfigured, isLoading, configure, lastError } = useSuperwallStore(
    useShallow((state) => ({
      isConfigured: state.isConfigured,
      isLoading: state.isLoading,
      configure: state.configure,
      lastError: state.lastError,
    })),
  )

  useEffect(() => {
    if (!isConfigured && !isLoading) {
      configure(apiKey, options).catch((err) => {
        console.error("Superwall configure failed", err)
      })
    }
  }, [isConfigured, isLoading, apiKey, options, configure])

  useEffect(() => {
    const cleanup = useSuperwallStore.getState()._initListeners()

    return cleanup
  }, [])

  if (lastError) {
    console.error("Superwall Error: ", lastError)
  }

  return <SuperwallContext.Provider value={true}>{children}</SuperwallContext.Provider>
}
