import { type ReactNode, useEffect } from "react"
import { Platform } from "react-native"
import { useShallow } from "zustand/shallow"
import { useCustomPurchaseController } from "./CustomPurchaseControllerProvider"
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
 * Main provider component for the Superwall SDK.
 *
 * This component initializes the Superwall SDK with your API key and configuration options.
 * It should wrap the root of your application or the part of your app that requires Superwall functionality.
 * It also sets up necessary event listeners for the SDK.
 *
 * @param props - The properties for the SuperwallProvider.
 * @param props.apiKeys - An object containing your Superwall API keys for Android and iOS.
 * @param props.options - Optional configuration options to pass to the native Superwall SDK.
 * @param props.children - The child components of your application that will have access to Superwall features.
 *
 * Example:
 * ```tsx
 * <SuperwallProvider apiKeys={{ ios: "YOUR_IOS_API_KEY", android: "YOUR_ANDROID_API_KEY" }}>
 *   <App />
 * </SuperwallProvider>
 * ```
 */
export function SuperwallProvider({
  apiKeys,
  options,

  children,
}: SuperwallProviderProps) {
  const isUsingCustomPurchaseController = !!useCustomPurchaseController()
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

      configure(apiKey, {
        ...options,
        manualPurchaseManagment: isUsingCustomPurchaseController,
      }).catch((err) => {
        console.error("Superwall configure failed", err)
      })
    }
  }, [isConfigured, isUsingCustomPurchaseController, isLoading, apiKeys, options, configure])

  useEffect(() => {
    const cleanup = useSuperwallStore.getState()._initListeners()

    return cleanup
  }, [])

  return <SuperwallContext.Provider value={true}>{children}</SuperwallContext.Provider>
}
