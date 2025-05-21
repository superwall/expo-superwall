export type SuperwallExpoModuleEvents = {
  onPaywallPresent: (params: string) => void
  onPaywallDismiss: (params: string) => void
  onPaywallError: (params: string) => void
  onPaywallSkip: (params: string) => void
}
