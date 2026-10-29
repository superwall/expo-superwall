package expo.modules.superwallexpo.json

import com.superwall.sdk.analytics.superwall.AttributionMatchInfo

fun AttributionMatchInfo.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>(
    "provider" to provider.rawName,
    "matched" to matched
  )
  source?.let { map["source"] = it }
  confidence?.let { map["confidence"] = it.rawName }
  matchScore?.let { map["matchScore"] = it }
  reason?.let { map["reason"] = it }
  return map
}
