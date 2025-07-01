import { createContext, useContext } from "react"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type { OnPurchaseParams } from "./SuperwallExpoModule.types"
import { useSuperwallEvents } from "./useSuperwallEvents"

const customPurchaseControllerContext = createContext<CustomPurchaseControllerContext | null>(null)

/**
 * @category Purchase Controller
 * @since 0.0.15
 */
export interface PurchaseResult {
  type: string
  error?: string
}

/**
 * @category Purchase Controller
 * @since 0.0.15
 */
export interface CustomPurchaseControllerContext {
  onPurchase: (params: OnPurchaseParams) => Promise<void>
  onPurchaseRestore: () => Promise<void>
}

/**
 * @category Purchase Controller
 * @since 0.0.15
 */
export interface CustomPurchaseControllerProviderProps {
  children: React.ReactNode
  controller: CustomPurchaseControllerContext
}

/**
 * @category Purchase Controller
 * @since 0.0.15
 * Provider component for custom purchase controller logic.
 *
 * This component allows you to integrate your own purchase handling logic
 * with the Superwall SDK. It listens for purchase and restore events from
 * Superwall and delegates them to the provided `controller`.
 *
 * @param props - The properties for the CustomPurchaseControllerProvider.
 * @param props.children - The child components that will be wrapped by this provider.
 * @param props.controller - An object implementing the `CustomPurchaseControllerContext`
 *                           interface, which defines how to handle purchases and restores.
 */
export const CustomPurchaseControllerProvider = ({
  children,
  controller,
}: CustomPurchaseControllerProviderProps) => {
  useSuperwallEvents({
    onPurchase: async (params) => {
      await controller.onPurchase(params)

      SuperwallExpoModule.didPurchase({
        type: "purchased",
      })
    },
    onPurchaseRestore: async () => {
      await controller.onPurchaseRestore()

      SuperwallExpoModule.didRestore({
        type: "restored",
      })
    },
  })

  return (
    <customPurchaseControllerContext.Provider value={controller}>
      {children}
    </customPurchaseControllerContext.Provider>
  )
}

/**
 * @category Purchase Controller
 * @since 0.0.15
 * Hook to access the custom purchase controller context.
 *
 * This hook provides access to the `controller` object that was passed
 * to the `CustomPurchaseControllerProvider`. It can be used by child
 * components to trigger purchase or restore flows using the custom logic.
 *
 * @returns The `CustomPurchaseControllerContext` object, or `null` if not
 *          used within a `CustomPurchaseControllerProvider`.
 */
export const useCustomPurchaseController = () => {
  const context = useContext(customPurchaseControllerContext)

  return context
}
