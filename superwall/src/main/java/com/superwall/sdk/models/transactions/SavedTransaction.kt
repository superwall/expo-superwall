package com.superwall.sdk.models.transactions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The transaction to save to storage
 * @param id The id of the transaction
 * @param date The date of the transaction as unix epoch time
 * @param hasExternalPurchaseController Whether the transaction has an external purchase controller
 * @param isExternal Whether the transaction is external
 **/
@Serializable
class SavedTransaction(
    @SerialName("id")
    val id: String,
    @SerialName("date")
    val date: Long,
    @SerialName("hasExternalPurchaseController")
    val hasExternalPurchaseController: Boolean,
    @SerialName("isExternal")
    val isExternal: Boolean,
)
