export type SuperwallExpoModuleEvents = {
  // Paywall Events
  onPaywallPresent: (params: any) => void
  onPaywallDismiss: (params: any) => void
  onPaywallError: (params: any) => void
  onPaywallSkip: (params: any) => void

  // --- SuperwallDelegateBridge Events ---
  subscriptionStatusDidChange: (params: any) => void
  handleSuperwallEvent: (params: any) => void
  handleCustomPaywallAction: (params: any) => void
  willDismissPaywall: (params: any) => void
  willPresentPaywall: (params: any) => void
  didDismissPaywall: (params: any) => void
  didPresentPaywall: (params: any) => void
  paywallWillOpenURL: (params: any) => void
  paywallWillOpenDeepLink: (params: any) => void
  handleLog: (params: any) => void
  willRedeemLink: (params: any) => void
  didRedeemLink: (params: any) => void

  // Purchase Events
  onPurchase: (
    params:
      | { productId: string; platform: "ios" }
      | { productId: string; basePlanId: string; offerId: string; platform: "android" },
  ) => void
  onPurchaseRestore: (params: any) => void
}
