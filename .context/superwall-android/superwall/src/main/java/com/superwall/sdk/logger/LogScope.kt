package com.superwall.sdk.logger

enum class LogScope {
    localizationManager,
    bounceButton,
    webEntitlements,
    customerInfo,
    coreData,
    configManager,
    identityManager,
    debugManager,
    debugView,
    localizationView,
    gameControllerManager,
    device,
    network,
    paywallEvents,
    productsManager,
    storeKitManager,
    placements,
    receipts,
    superwallCore,
    transactions,
    jsEvaluator,
    paywallPresentation,
    paywallTransactions,
    paywallView,
    nativePurchaseController,
    cache,
    deepLinks,
    all,
    ;

    override fun toString(): String = name
}
