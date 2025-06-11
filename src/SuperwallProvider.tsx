import { type ReactNode, useEffect } from "react"
import { useShallow } from "zustand/shallow"
import { SuperwallContext, useSuperwallStore } from "./useSuperwall"

interface SuperwallProviderProps {
  /** Your Superwall API key */
  apiKey: string
  /** Optional configuration options passed to the native SDK */
  options?: Record<string, any>
  /** Whether you are supplying a custom purchase controller */
  usingPurchaseController?: boolean
  /** SDK version override (rarely needed) */
  sdkVersion?: string
  /** Fallback React node shown while the SDK is configuring */
  fallback?: ReactNode
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
  usingPurchaseController,
  sdkVersion,
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
      configure(apiKey, options, usingPurchaseController, sdkVersion).catch((err) => {
        console.error("Superwall configure failed", err)
      })
    }
  }, [isConfigured, isLoading, apiKey, options, usingPurchaseController, sdkVersion, configure])

  useEffect(() => {
    const cleanup = useSuperwallStore.getState()._initListeners()
    return cleanup
  }, [])

  if (lastError) {
    console.error("Superwall Error: ", lastError)
  }

  return <SuperwallContext.Provider value={true}>{children}</SuperwallContext.Provider>
}
