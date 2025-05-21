package expo.modules.settings

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class SuperwallExpoModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("SuperwallExpo")

    Function("getTheme") {
      return@Function "system"
    }
  }
}