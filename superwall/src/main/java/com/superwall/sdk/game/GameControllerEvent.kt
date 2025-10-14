package com.superwall.sdk.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameControllerEvent(
    @SerialName("event_name")
    var eventName: String = "game_controller_input",
    @SerialName("controller_element")
    var controllerElement: String,
    @SerialName("value")
    var value: Double,
    @SerialName("x")
    var x: Double,
    @SerialName("y")
    var y: Double,
    @SerialName("directional")
    var directional: Boolean,
)
