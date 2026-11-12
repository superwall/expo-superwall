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
  StoreKitManager = "storeKitManager",
  Placements = "placements",
  Receipts = "receipts",
  SuperwallCore = "superwallCore",
  PaywallPresentation = "paywallPresentation",
  PaywallTransactions = "paywallTransactions",
  PaywallViewController = "paywallViewController",
  Cache = "cache",
  /** Android only. */
  Billing = "billing",
  /** iOS only. */
  Analytics = "analytics",
  Transactions = "transactions",
  WebEntitlements = "webEntitlements",
  /** iOS only. */
  GrantedEntitlements = "grantedEntitlements",
  CustomerCenter = "customerCenter",
  /** Android only. */
  CustomerInfo = "customerInfo",
  /** Android only. */
  JsEvaluator = "jsEvaluator",
  /** Android only. */
  NativePurchaseController = "nativePurchaseController",
  /** Android only. */
  DeepLinks = "deepLinks",
  All = "all",
}
