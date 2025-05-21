import ExpoModulesCore

public class SuperwallExpoModule: Module {
  public func definition() -> ModuleDefinition {
    Name("SuperwallExpo")

    Function("getTheme") { () -> String in
      "system"
    }
  }
}