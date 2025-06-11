import { type ReactNode, useEffect } from "react"
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
  fallback = null,
  children,
}: SuperwallProviderProps) {
  const { configured, loading, configure, lastError } = useSuperwallStore()

  // Configure the SDK once on mount
  useEffect(() => {
    if (!configured && !loading) {
      configure(apiKey, options, usingPurchaseController, sdkVersion).catch((err) => {
        console.error("Superwall configure failed", err)
      })
    }
  }, [configured, loading, apiKey, options, usingPurchaseController, sdkVersion, configure])

  // Setup native event listeners once
  useEffect(() => {
    const cleanup = useSuperwallStore.getState()._initListeners()
    return cleanup
  }, [])

  if (lastError) {
    console.error("Superwall Error: ", lastError)
  }

  return (
    <SuperwallContext.Provider value={true}>
      {configured ? children : fallback}
    </SuperwallContext.Provider>
  )
}
