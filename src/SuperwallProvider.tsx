import { type ReactNode, useEffect, useRef } from "react"
import { type EmitterSubscription, Linking, Platform } from "react-native"
import { useShallow } from "zustand/shallow"
import { useCustomPurchaseController } from "./CustomPurchaseControllerProvider"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type { SuperwallOptions } from "./SuperwallOptions"
import { SuperwallContext, useSuperwallStore } from "./useSuperwall"

interface SuperwallProviderProps {
  /** Your Superwall API key */
  apiKeys: {
    android?: string
    ios?: string
  }
  /** Optional configuration options passed to the native SDK */
  options?: Partial<SuperwallOptions> & {
    /** @deprecated Use manualPurchaseManagement instead */
    manualPurchaseManagment?: boolean
  }
  /** App content to render once configured */
  children: ReactNode
}

/**
 * Checks if a URL is an Expo deep link scheme.
 * Matches expo://, exp://, or exp+{slug}:// schemes.
 *
 * @param url - The URL string to check
 * @returns true if the URL is an Expo deep link scheme, false otherwise
 */
const isExpoDeepLink = (url: string): boolean => {
  const re = /^(?:expo|exp\+[^:]+|exp):\/\//i
  return re.test(url)
}

/**
 * Checks if a URL is an Expo platform URL (expo.dev domain).
 *
 * @param url - The URL string to check
 * @returns true if the URL is an Expo platform URL, false otherwise
 */
const isExpoPlatformUrl = (url: string): boolean => {
  const re = /^https?:\/\/(?:www\.)?expo\.dev\//i
  return re.test(url)
}

/**
 * @category Providers
 * @since 0.0.15
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
  const deepLinkEventHandlerRef = useRef<EmitterSubscription>(null)
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
        manualPurchaseManagement: isUsingCustomPurchaseController,
      }).catch((err) => {
        console.error("Superwall configure failed", err)
      })
    }
  }, [isConfigured, isUsingCustomPurchaseController, isLoading, apiKeys, options, configure])

  useEffect(() => {
    const cleanup = useSuperwallStore.getState()._initListeners()

    return cleanup
  }, [])

  useEffect(() => {
    const handleDeepLink = async () => {
      await Linking.getInitialURL().then((url) => {
        if (url && !isExpoDeepLink(url) && !isExpoPlatformUrl(url)) {
          SuperwallExpoModule.handleDeepLink(url)
        }
      })

      deepLinkEventHandlerRef.current = Linking.addEventListener("url", (event) => {
        if (!isExpoDeepLink(event.url) && !isExpoPlatformUrl(event.url)) {
          SuperwallExpoModule.handleDeepLink(event.url)
        }
      })
    }

    handleDeepLink()

    return () => {
      if (deepLinkEventHandlerRef.current) {
        deepLinkEventHandlerRef.current.remove()
      }
    }
  }, [])

  return <SuperwallContext.Provider value={true}>{children}</SuperwallContext.Provider>
}
