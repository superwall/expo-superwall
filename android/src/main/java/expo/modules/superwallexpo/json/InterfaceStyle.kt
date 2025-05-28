package expo.modules.superwallexpo.json


import com.superwall.sdk.network.device.InterfaceStyle

fun interfaceStyleFromString(interfaceStyle: String): InterfaceStyle {
    return when (interfaceStyle) {
    "LIGHT" -> InterfaceStyle.LIGHT
    "DARK" -> InterfaceStyle.DARK
    else -> InterfaceStyle.LIGHT // Default case to handle unexpected values
    }
}

