import { createContext, useContext } from "react"
import SuperwallExpoModule from "./SuperwallExpoModule"
import type { OnPurchaseParams } from "./SuperwallExpoModule.types"
import { useSuperwallEvents } from "./useSuperwallEvents"

const customPurchaseControllerContext = createContext<CustomPurchaseControllerContext | null>(null)

export interface PurchaseResult {
  type: string
  error?: string
}

export interface CustomPurchaseControllerContext {
  onPurchase: (params: OnPurchaseParams) => Promise<void>
  onPurchaseRestore: () => Promise<void>
}

export interface CustomPurchaseControllerProviderProps {
  children: React.ReactNode
  controller: CustomPurchaseControllerContext
}

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

export const useCustomPurchaseController = () => {
  const context = useContext(customPurchaseControllerContext)

  return context
}
