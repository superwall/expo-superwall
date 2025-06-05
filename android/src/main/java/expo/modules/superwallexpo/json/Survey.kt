package expo.modules.superwallexpo.json

import com.superwall.sdk.config.models.Survey
import com.superwall.sdk.config.models.SurveyOption
import com.superwall.sdk.config.models.SurveyShowCondition

fun Survey.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()

  map["id"] = this.id
  map["assignmentKey"] = this.assignmentKey
  map["title"] = this.title
  map["message"] = this.message

  val optionsArray = mutableListOf<Map<String, Any>>()
  this.options.forEach {
    optionsArray.add(it.toJson())
  }
  map["options"] = optionsArray

  map["presentationCondition"] = this.presentationCondition.toJson()
  map["presentationProbability"] = this.presentationProbability
  map["includeOtherOption"] = this.includeOtherOption
  map["includeCloseOption"] = this.includeCloseOption

  return map
}

fun SurveyOption.toJson(): Map<String, Any> {
  val map = mutableMapOf<String, Any>()

  map["id"] = this.id
  map["title"] = this.title

  return map
}

fun SurveyShowCondition.toJson(): String {
  return when (this) {
    SurveyShowCondition.ON_MANUAL_CLOSE -> "ON_MANUAL_CLOSE"
    SurveyShowCondition.ON_PURCHASE -> "ON_PURCHASE"
  }
}
