/**
 * @category Enums
 * @since 0.0.15
 * Defines the scopes for logging within the SDK.
 */
export enum LogScope {
  LocalizationManager = "localizationManager",
  BounceButton = "bounceButton",
  CoreData = "coreData",
  ConfigManager = "configManager",
  IdentityManager = "identityManager",
  DebugManager = "debugManager",
  DebugViewController = "debugViewController",
  LocalizationViewController = "localizationViewController",
  GameControllerManager = "gameControllerManager",
  Device = "device",
  Network = "network",
  PaywallEvents = "paywallEvents",
  ProductsManager = "productsManager",
  /**
   * StoreKit logs on iOS. On Android this is a deprecated alias for {@link LogScope.Billing}
   * and still enables the Google Play Billing logs.
   */
  StoreKitManager = "storeKitManager",
  /**
   * Google Play Billing logs on Android (Android SDK 2.10.0+). On Android, billing logs passed to
   * `handleLog` now have the scope `"billing"` instead of `"storeKitManager"`.
   * On iOS, this enables the `storeKitManager` (StoreKit) logs.
   */
  Billing = "billing",
  Placements = "placements",
  Receipts = "receipts",
  SuperwallCore = "superwallCore",
  PaywallPresentation = "paywallPresentation",
  PaywallTransactions = "paywallTransactions",
  PaywallViewController = "paywallViewController",
  Cache = "cache",
  All = "all",
}
