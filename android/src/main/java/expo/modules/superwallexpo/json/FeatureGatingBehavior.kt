package expo.modules.superwallexpo.json

import com.superwall.sdk.models.config.FeatureGatingBehavior

fun FeatureGatingBehavior.rawValue(): String {
  return when (this) {
    FeatureGatingBehavior.Gated -> "gated"
    FeatureGatingBehavior.NonGated -> "nonGated"
  }
}
