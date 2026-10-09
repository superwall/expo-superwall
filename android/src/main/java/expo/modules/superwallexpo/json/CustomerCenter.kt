package expo.modules.superwallexpo.json

import com.superwall.sdk.customercenter.CustomerCenterAction
import com.superwall.sdk.customercenter.CustomerCenterConfiguration
import com.superwall.sdk.customercenter.CustomerCenterPurchase
import com.superwall.sdk.customercenter.CustomerCenterRefundStatus
import com.superwall.sdk.customercenter.CustomerCenterScreenType

// Builds a configuration from the JS `CustomerCenterConfiguration`. Anything left out keeps
// the native default, so `{}` is the same as `CustomerCenterConfiguration.default`.
@Suppress("UNCHECKED_CAST")
fun customerCenterConfigurationFromJson(json: Map<String, Any?>): CustomerCenterConfiguration {
  val default = CustomerCenterConfiguration.default
  return CustomerCenterConfiguration(
    managementScreen = (json["managementScreen"] as? Map<String, Any?>)
      ?.let { customerCenterScreenFromJson(it) } ?: default.managementScreen,
    noPurchasesScreen = (json["noPurchasesScreen"] as? Map<String, Any?>)
      ?.let { customerCenterScreenFromJson(it) } ?: default.noPurchasesScreen,
    support = (json["support"] as? Map<String, Any?>)
      ?.let { customerCenterSupportFromJson(it) } ?: default.support,
    appearance = (json["appearance"] as? Map<String, Any?>)
      ?.let { customerCenterAppearanceFromJson(it) } ?: default.appearance,
    showsAccountDetails = json["showsAccountDetails"] as? Boolean ?: default.showsAccountDetails,
    warnsAboutDuplicateSubscriptions = json["warnsAboutDuplicateSubscriptions"] as? Boolean
      ?: default.warnsAboutDuplicateSubscriptions,
  )
}

@Suppress("UNCHECKED_CAST")
private fun customerCenterScreenFromJson(json: Map<String, Any?>): CustomerCenterConfiguration.Screen {
  val paths = (json["paths"] as? List<Any?>)
    ?.mapNotNull { (it as? Map<String, Any?>)?.let { path -> customerCenterPathFromJson(path) } }
    ?: emptyList()
  return CustomerCenterConfiguration.Screen(
    title = json["title"] as? String,
    subtitle = json["subtitle"] as? String,
    paths = paths,
  )
}

// Returns null for an unknown `type`, or a url/custom path missing its required field, so a
// bad entry is dropped rather than failing the whole configuration.
@Suppress("UNCHECKED_CAST")
private fun customerCenterPathFromJson(json: Map<String, Any?>): CustomerCenterConfiguration.Path? {
  val type: CustomerCenterConfiguration.PathType = when (json["type"] as? String) {
    "restore" -> CustomerCenterConfiguration.PathType.Restore
    "manageSubscription" -> CustomerCenterConfiguration.PathType.ManageSubscription
    "refund" -> CustomerCenterConfiguration.PathType.Refund(
      windowMillis = (json["windowMillis"] as? Number)?.toLong()
    )
    "changePlan" -> CustomerCenterConfiguration.PathType.ChangePlan(
      productIds = (json["productIds"] as? List<Any?>)?.filterIsInstance<String>()
    )
    "contactSupport" -> CustomerCenterConfiguration.PathType.ContactSupport
    "url" -> {
      val url = json["url"] as? String ?: return null
      val openMethod = when (json["openMethod"] as? String) {
        "external" -> CustomerCenterConfiguration.OpenMethod.EXTERNAL
        else -> CustomerCenterConfiguration.OpenMethod.IN_APP
      }
      CustomerCenterConfiguration.PathType.Url(url, openMethod)
    }
    "custom" -> {
      val identifier = json["identifier"] as? String ?: return null
      CustomerCenterConfiguration.PathType.Custom(identifier)
    }
    else -> return null
  }
  return CustomerCenterConfiguration.Path(
    type = type,
    title = json["title"] as? String,
    survey = (json["survey"] as? Map<String, Any?>)?.let { customerCenterSurveyFromJson(it) },
    id = json["id"] as? String ?: type.defaultId,
  )
}

@Suppress("UNCHECKED_CAST")
private fun customerCenterSurveyFromJson(
  json: Map<String, Any?>
): CustomerCenterConfiguration.FeedbackSurvey? {
  val id = json["id"] as? String ?: return null
  val options = (json["options"] as? List<Any?>)
    ?.mapNotNull { option ->
      val map = option as? Map<String, Any?> ?: return@mapNotNull null
      val optionId = map["id"] as? String ?: return@mapNotNull null
      CustomerCenterConfiguration.FeedbackSurvey.Option(id = optionId, title = map["title"] as? String)
    }
    ?: emptyList()
  return CustomerCenterConfiguration.FeedbackSurvey(
    id = id,
    title = json["title"] as? String,
    options = options,
  )
}

private fun customerCenterSupportFromJson(json: Map<String, Any?>): CustomerCenterConfiguration.Support {
  val default = CustomerCenterConfiguration.Support()
  return CustomerCenterConfiguration.Support(
    email = json["email"] as? String,
    latestAppVersion = json["latestAppVersion"] as? String,
    warnsAboutUpdates = json["warnsAboutUpdates"] as? Boolean ?: default.warnsAboutUpdates,
    webManagementUrl = json["webManagementUrl"] as? String,
  )
}

@Suppress("UNCHECKED_CAST")
private fun customerCenterAppearanceFromJson(json: Map<String, Any?>): CustomerCenterConfiguration.Appearance {
  val accent = (json["accent"] as? Map<String, Any?>)?.let {
    val light = it["light"] as? String
    val dark = it["dark"] as? String
    if (light != null && dark != null) {
      CustomerCenterConfiguration.Appearance.ColorPair(light = light, dark = dark)
    } else {
      null
    }
  }
  return CustomerCenterConfiguration.Appearance(accent = accent)
}

fun CustomerCenterAction.toJson(): Map<String, Any> =
  when (this) {
    CustomerCenterAction.Restore -> mapOf("type" to "restore")
    CustomerCenterAction.ManageSubscription -> mapOf("type" to "manageSubscription")
    CustomerCenterAction.Refund -> mapOf("type" to "refund")
    CustomerCenterAction.ChangePlan -> mapOf("type" to "changePlan")
    CustomerCenterAction.ContactSupport -> mapOf("type" to "contactSupport")
    is CustomerCenterAction.Url -> mapOf("type" to "url", "url" to url)
    is CustomerCenterAction.Custom -> mapOf("type" to "custom", "identifier" to identifier)
  }

fun CustomerCenterRefundStatus.toJson(): String =
  when (this) {
    CustomerCenterRefundStatus.SUCCESS -> "success"
    CustomerCenterRefundStatus.USER_CANCELLED -> "userCancelled"
    CustomerCenterRefundStatus.ERROR -> "error"
  }

fun CustomerCenterScreenType.toJson(): String =
  when (this) {
    CustomerCenterScreenType.MANAGEMENT -> "management"
    CustomerCenterScreenType.NO_PURCHASES -> "noPurchases"
  }

fun CustomerCenterPurchase.toJson(): Map<String, Any?> {
  val map = mutableMapOf<String, Any?>()
  map["productId"] = productId
  map["store"] = store.name
  map["entitlements"] = entitlements.toJson()
  subscription?.let { map["subscription"] = it.toJson() }
  nonSubscription?.let { map["nonSubscription"] = it.toJson() }
  return map
}
