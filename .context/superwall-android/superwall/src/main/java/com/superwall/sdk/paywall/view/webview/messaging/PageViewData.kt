package com.superwall.sdk.paywall.view.webview.messaging

data class PageViewData(
    val pageNodeId: String,
    val flowPosition: Int,
    val pageName: String,
    val navigationNodeId: String,
    val previousPageNodeId: String?,
    val previousFlowPosition: Int?,
    val navigationType: String,
    val timeOnPreviousPageMs: Int?,
)
