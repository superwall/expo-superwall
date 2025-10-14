package com.superwall.sdk.misc

import android.content.Context
import java.io.IOException

class VersionHelper(
    val context: Context,
) {
    val gitSha: String?
    val buildTime: String?
    val sdkVersion: String

    init {
        gitSha = readFileFromAssets(context, "git_sha.txt")
        buildTime = readFileFromAssets(context, "build_time.txt")
        this.sdkVersion = SDK_VERSION
    }

    private fun readFileFromAssets(
        context: Context,
        fileName: String,
    ): String? {
        val assetManager = context.assets
        return try {
            val inputStream = assetManager.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close() // Close the InputStream
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
