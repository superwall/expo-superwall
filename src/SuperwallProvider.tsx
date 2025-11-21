import { type ReactNode, useEffect, useRef } from "react"
import { type EmitterSubscription, Linking, Platform } from "react-native"
import { useShallow } from "zustand/shallow"
import { useCustomPurchaseController } from "./CustomPurchaseControllerProvider"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type { PartialSuperwallOptions } from "./SuperwallOptions"
import { SuperwallContext, useSuperwallStore } from "./useSuperwall"

interface SuperwallProviderProps {
  /** Your Superwall API key */
  apiKeys: {
    android?: string
    ios?: string
  }
  /** Optional configuration options passed to the native SDK */
  options?: PartialSuperwallOptions & {
    /** @deprecated Use manualPurchaseManagement instead */
    manualPurchaseManagment?: boolean
  }
  /** App content to render once configured */
  children: ReactNode
  /**
   * Optional callback invoked when SDK configuration fails.
   * Use this to track errors, show custom UI, or implement retry logic.
   * @param error - The error that occurred during configuration
   */
  onConfigurationError?: (error: Error) => void
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
  onConfigurationError,
}: SuperwallProviderProps) {
  const deepLinkEventHandlerRef = useRef<EmitterSubscription>(null)
  const isUsingCustomPurchaseController = !!useCustomPurchaseController()

  const { isConfigured, isLoading, configure, configurationError } = useSuperwallStore(
    useShallow((state) => ({
      isConfigured: state.isConfigured,
      isLoading: state.isLoading,
      configure: state.configure,
      configurationError: state.configurationError,
    })),
  )

  useEffect(() => {
    if (!isConfigured && !isLoading && !configurationError) {
      const apiKey = apiKeys[Platform.OS as keyof typeof apiKeys]
      if (!apiKey) {
        const error = new Error(`No API key provided for platform ${Platform.OS}`)
        console.error("Superwall configure failed", error)
        onConfigurationError?.(error)
        return
      }

      configure(apiKey, {
        ...options,
        manualPurchaseManagement: isUsingCustomPurchaseController,
      })
    }
  }, [
    isConfigured,
    isUsingCustomPurchaseController,
    isLoading,
    configurationError,
    apiKeys,
    options,
    configure,
    onConfigurationError,
  ])

  // Notify callback when configuration error changes
  useEffect(() => {
    if (configurationError && onConfigurationError) {
      onConfigurationError(new Error(configurationError))
    }
  }, [configurationError, onConfigurationError])

  useEffect(() => {
    const cleanup = useSuperwallStore.getState()._initListeners()

    return cleanup
  }, [])

  useEffect(() => {
    const handleDeepLink = async () => {
      try {
        const url = await Linking.getInitialURL()
        if (url && !isExpoDeepLink(url) && !isExpoPlatformUrl(url)) {
          SuperwallExpoModule.handleDeepLink(url).catch((error) => {
            console.debug("Superwall: Non-Superwall deep link ignored", url, error)
          })
        }
      } catch (error) {
        console.debug("Superwall: Failed to get initial URL", error)
      }

      deepLinkEventHandlerRef.current = Linking.addEventListener("url", (event) => {
        if (!isExpoDeepLink(event.url) && !isExpoPlatformUrl(event.url)) {
          SuperwallExpoModule.handleDeepLink(event.url).catch((error) => {
            console.debug("Superwall: Non-Superwall deep link ignored", event.url, error)
          })
        }
      })
    }

    handleDeepLink().catch((error) => {
      console.error("Superwall: Deep link setup failed", error)
    })

    return () => {
      if (deepLinkEventHandlerRef.current) {
        deepLinkEventHandlerRef.current.remove()
      }
    }
  }, [])

  return <SuperwallContext.Provider value={true}>{children}</SuperwallContext.Provider>
}
