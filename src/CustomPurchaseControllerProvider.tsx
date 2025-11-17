import { createContext, useContext } from "react"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type { OnPurchaseParams } from "./SuperwallExpoModule.types"
import { useSuperwallEvents } from "./useSuperwallEvents"

const customPurchaseControllerContext = createContext<CustomPurchaseControllerContext | null>(null)

/**
 * @category Purchase Controller
 * @since 0.0.15
 */
export type PurchaseResult = {
  type: "cancelled" | "failed" | "purchased" | "pending"
  error?: string
}

/**
 * @category Purchase Controller
 * @since 0.0.15
 */
export type RestoreResult = {
  type: "restored" | "failed"
  error?: string
}

/**
 * @category Purchase Controller
 * @since 0.0.15
 */
export interface CustomPurchaseControllerContext {
  // biome-ignore lint/suspicious/noConfusingVoidType: void is intentional to allow implicit returns for success
  onPurchase: (params: OnPurchaseParams) => Promise<PurchaseResult | void>
  // biome-ignore lint/suspicious/noConfusingVoidType: void is intentional to allow implicit returns for success
  onPurchaseRestore: () => Promise<RestoreResult | void>
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
      try {
        const result = await controller.onPurchase(params)
        const type = result?.type ?? "purchased"

        SuperwallExpoModule.didPurchase({
          type,
          ...(type === "failed" && { error: result?.error || "Unknown error" }),
        })
      } catch (error: any) {
        SuperwallExpoModule.didPurchase({
          type: "failed",
          error: error?.message || "Unknown error",
        })
      }
    },
    onPurchaseRestore: async () => {
      try {
        const result = await controller.onPurchaseRestore()
        const resultType = result?.type ?? "restored"

        SuperwallExpoModule.didRestore({
          result: resultType,
          ...(resultType === "failed" && { errorMessage: result?.error || "Unknown error" }),
        })
      } catch (error: any) {
        SuperwallExpoModule.didRestore({
          result: "failed",
          errorMessage: error?.message || "Unknown error",
        })
      }
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
