import { createContext, useContext } from "react"
import SuperwallExpoModule from "./SuperwallExpoModule"
import { useSuperwallStore } from "./useSuperwall"
import { useSuperwallEvents } from "./useSuperwallEvents"

const customPurchaseControllerContext = createContext<CustomPurchaseControllerContext | null>(null)

export interface PurchaseResult {
  type: string
  error?: string
}

export interface CustomPurchaseControllerContext {
  onPurchase: (params: any) => Promise<void>
  onPurchaseRestore: (params: any) => Promise<void>
}

export interface CustomPurchaseControllerProviderProps {
  children: React.ReactNode
  controller: CustomPurchaseControllerContext
}

export const CustomPurchaseControllerProvider = ({
  children,
  controller,
}: CustomPurchaseControllerProviderProps) => {
  const store = useSuperwallStore()
  useSuperwallEvents({
    onPurchase: async (params) => {
      await controller.onPurchase(params)

      SuperwallExpoModule.didPurchase({
        type: "purchased",
      })
    },
    onPurchaseRestore: async () => {
      await controller.onPurchaseRestore({})

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
