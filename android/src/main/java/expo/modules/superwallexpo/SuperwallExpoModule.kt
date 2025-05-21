package expo.modules.settings

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class SuperwallExpoModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("SuperwallExpo")

    Function("getTheme") {
      return@Function "system"
    }

    Function("getApiKey") {
      val applicationInfo = appContext?.reactContext?.packageManager?.getApplicationInfo(appContext?.reactContext?.packageName.toString(), PackageManager.GET_META_DATA)

      return@Function applicationInfo?.metaData?.getString("SUPERWALL_API_KEY")
    }
  }
}