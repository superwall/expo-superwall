package expo.modules.superwallexpo.json

import com.superwall.sdk.Superwall
import com.superwall.sdk.models.paywall.PaywallPresentationStyle
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.store.abstractions.product.StoreProduct

// Takes a style name, or `{ style, height, width, cornerRadius }` for a drawer or popup.
fun paywallPresentationStyleFromJson(json: Any?): PaywallPresentationStyle? {
  val map = json as? Map<*, *>
  val style = (json as? String) ?: (map?.get("style") as? String) ?: return null
  val height = (map?.get("height") as? Number)?.toDouble()
  val width = (map?.get("width") as? Number)?.toDouble()
  val cornerRadius = (map?.get("cornerRadius") as? Number)?.toDouble()

  return when (style) {
    "MODAL" -> PaywallPresentationStyle.Modal
    "FULLSCREEN" -> PaywallPresentationStyle.Fullscreen
    "NO_ANIMATION" -> PaywallPresentationStyle.FullscreenNoAnimation
    "PUSH" -> PaywallPresentationStyle.Push
    // The defaults match the ones iOS uses when none are given.
    "DRAWER" -> PaywallPresentationStyle.Drawer(height ?: 70.0, cornerRadius ?: 15.0)
    "POPUP" ->
      if (height != null && width != null) {
        PaywallPresentationStyle.Popup(height, width, cornerRadius ?: 15.0)
      } else {
        null
      }
    "NONE" -> PaywallPresentationStyle.None
    else -> null
  }
}

/**
 * Builds overrides from `{ productsByName: { name: productId }, presentationStyle }`.
 * Products the store doesn't return are left out, so the paywall keeps its own.
 */
suspend fun paywallOverridesFromJson(json: Map<String, Any?>): PaywallOverrides {
  val productIdsByName =
    (json["productsByName"] as? Map<*, *>)
      ?.mapNotNull { (name, productId) ->
        if (name is String && productId is String) name to productId else null
      }?.toMap()
      .orEmpty()

  val productsByName = mutableMapOf<String, StoreProduct>()
  if (productIdsByName.isNotEmpty()) {
    productIdsByName.values.distinct().forEach { productId ->
      // One at a time: an identifier the store doesn't know fails a whole batch.
      val product = Superwall.instance.getProducts(productId).getOrNull()?.get(productId)
      if (product != null) {
        productIdsByName.filterValues { it == productId }.keys.forEach { productsByName[it] = product }
      }
    }
  }

  return PaywallOverrides(
    productsByName = productsByName,
    presentationStyle =
      paywallPresentationStyleFromJson(json["presentationStyle"]) ?: PaywallPresentationStyle.None,
  )
}
