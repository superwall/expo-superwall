import { type ReactNode, createContext, useEffect, useState } from "react"
import { Platform } from "react-native"
import SuperwallExpoModule from "./SuperwallExpoModule"

import pkg from "../package.json"

export const SuperwallContext = createContext<null>(null)

export interface SuperwallProviderProps {
  children: ReactNode
  config: {
    ios?: {
      apiKey: string
    }
    android?: {
      apiKey: string
    }
  }
}

export const SuperwallProvider = ({ children, config }: SuperwallProviderProps) => {
  useEffect(() => {
    const apiKey = config[Platform.OS as "ios"]?.apiKey

    if (!apiKey) {
      throw new Error(`Please provide an Superwall apiKey for ${Platform.OS}`)
    }

    console.info("Configuring Superwall")

    const xd = SuperwallExpoModule.addListener("onPaywallPresent", () => {
      console.info("Paywall presented")
    })
    SuperwallExpoModule.configure(apiKey, {}, true, pkg.version)

    console.info("Superwall configured")
  }, [config])

  return <SuperwallContext.Provider value={null}>{children}</SuperwallContext.Provider>
}

export const useUser = () => {
  const [isIdentified, setIsIdentified] = useState(false)
}

export const useSuperwall = () => {}
