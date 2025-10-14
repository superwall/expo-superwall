package com.superwall.sdk.misc

import java.math.BigInteger
import java.security.MessageDigest

fun String.sha256(): ByteArray? =
    try {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(this.toByteArray(Charsets.UTF_8))
        digest.digest()
    } catch (e: Throwable) {
        null
    }

fun String.sha256MappedToRange(): Int? {
    val hashBytes = this.sha256() ?: return null

    // Break the hash into 8-byte chunks
    val chunks =
        hashBytes
            .toList()
            .windowed(8, 8)
            .map { it.toByteArray() }

    // Sum the modulo 100 value of each chunk
    var sum = BigInteger.ZERO
    for (chunk in chunks) {
        val chunkValue = BigInteger(chunk)
        sum = sum.add(chunkValue)
    }

    return (sum.mod(BigInteger("100"))).toInt()
}
