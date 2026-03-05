package com.superwall.sdk.store.testmode.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TestStoreUser(
    val type: TestStoreUserType,
    val value: String,
)

@Serializable
enum class TestStoreUserType {
    @SerialName("userId")
    UserId,

    @SerialName("aliasId")
    AliasId,
}
