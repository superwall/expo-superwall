package com.superwall.sdk.models.internal

import java.util.UUID

@JvmInline
value class VendorId(
    val value: String,
) {
    override fun toString(): String = value
}

class DeviceVendorId(
    vendorId: VendorId,
) {
    val value = "\$SuperwallDevice:${UUID.nameUUIDFromBytes(vendorId.value.toByteArray())}"

    val noPrefix = UUID.nameUUIDFromBytes(vendorId.value.toByteArray())

    override fun toString(): String = value
}
