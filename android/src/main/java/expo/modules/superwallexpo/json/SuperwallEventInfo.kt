package expo.modules.superwallexpo.json


import com.superwall.sdk.analytics.superwall.SuperwallEventInfo

fun SuperwallEventInfo.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  map["event"] = SuperwallEvent.toJson(this.event)
  map["params"] = this.params
  return map
}
