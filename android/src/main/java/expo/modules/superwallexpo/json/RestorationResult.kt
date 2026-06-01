package expo.modules.superwallexpo.json


import com.superwall.sdk.delegate.RestorationResult

fun restorationResultFromJson(json: Map<String, Any>): RestorationResult {
    return when (json["result"] as String) {
        "restored" -> RestorationResult.Restored()
        "failed" -> {
          val errorMessage = (json["errorMessage"] as String?) ?: "Unknown error"
          RestorationResult.Failed(Error(errorMessage))
        }
        else -> RestorationResult.Failed(Error("Unknown restoration result"))
      }
}

fun restorationResultToJson(result: RestorationResult): Map<String, Any?> {
    return when (result) {
        is RestorationResult.Restored -> mapOf("result" to "restored")
        is RestorationResult.Failed -> mapOf(
            "result" to "failed",
            "errorMessage" to (result.error?.message ?: "Unknown error")
        )
    }
}
