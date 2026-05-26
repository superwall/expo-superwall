package com.superwall.sdk.paywall.view.webview

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Represents a local resource that can be served to paywall WebViews via `swlocal://` URLs.
 */
sealed class PaywallResource {
    /**
     * A resource backed by an Android [Uri] (`file://`, `content://`, etc.).
     */
    data class FromUri(
        val uri: Uri,
    ) : PaywallResource()

    /**
     * A resource backed by an Android resource ID (e.g. `R.raw.hero_video`, `R.drawable.bg`).
     */
    data class FromResources(
        val resId: Int,
    ) : PaywallResource()
}

internal class LocalResourceHandler(
    private val context: Context,
    private val localResources: () -> Map<String, PaywallResource>,
) {
    companion object {
        private const val SCHEME = "swlocal"
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }

    fun isLocalResourceUrl(url: Uri): Boolean = url.scheme == SCHEME

    fun handleRequest(url: Uri): WebResourceResponse {
        val resourceId = url.host
        if (resourceId.isNullOrEmpty()) {
            Logger.debug(
                LogLevel.error,
                LogScope.paywallView,
                "swlocal:// URL has no resource ID: $url",
            )
            return errorResponse(400, "Bad Request", "Missing resource ID in swlocal:// URL")
        }

        val resource = localResources()[resourceId]
        if (resource == null) {
            Logger.debug(
                LogLevel.error,
                LogScope.paywallView,
                "No local resource found for ID: $resourceId",
            )
            return errorResponse(404, "Not Found", "No local resource mapped for ID: $resourceId")
        }

        return when (resource) {
            is PaywallResource.FromUri -> handleUri(resourceId, resource.uri)
            is PaywallResource.FromResources -> handleAndroidResource(resourceId, resource.resId)
        }
    }

    private fun handleUri(
        resourceId: String,
        uri: Uri,
    ): WebResourceResponse {
        val mimeType = resolveMimeType(uri)
        val inputStream =
            openStreamOrError(resourceId, uri) ?: return errorResponse(500, "Internal Error", "Failed to read resource: $resourceId")
        return successResponse(mimeType, inputStream)
    }

    private fun handleAndroidResource(
        resourceId: String,
        resId: Int,
    ): WebResourceResponse {
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        val mimeType = resolveResourceMimeType(resId, uri)
        val inputStream =
            try {
                context.resources.openRawResource(resId)
            } catch (e: Exception) {
                Logger.debug(
                    LogLevel.error,
                    LogScope.paywallView,
                    "Failed to open Android resource '$resourceId' (resId=$resId)",
                    error = e,
                )
                return errorResponse(500, "Internal Error", "Failed to read resource: ${e.message}")
            }
        return successResponse(mimeType, inputStream)
    }

    private fun openStreamOrError(
        resourceId: String,
        uri: Uri,
    ): InputStream? =
        try {
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("ContentResolver returned null InputStream")
        } catch (e: Exception) {
            Logger.debug(
                LogLevel.error,
                LogScope.paywallView,
                "Failed to open local resource '$resourceId' at $uri",
                error = e,
            )
            null
        }

    private fun resolveMimeType(uri: Uri): String {
        context.contentResolver.getType(uri)?.let { return it }
        return mimeTypeFromExtension(uri.toString())
    }

    private fun resolveResourceMimeType(
        resId: Int,
        uri: Uri,
    ): String {
        context.contentResolver.getType(uri)?.let { return it }

        // Try to extract extension from the resource entry name (e.g. "hero_video" won't have one,
        // but the resource type name "raw"/"drawable" gives us a hint)
        try {
            val entryName = context.resources.getResourceEntryName(resId)
            val ext = entryName.substringAfterLast('.', "")
            if (ext.isNotEmpty()) {
                return mimeTypeFromExtension(entryName)
            }
        } catch (_: Exception) {
            // Resource not found - fall through
        }

        return DEFAULT_MIME_TYPE
    }

    private fun mimeTypeFromExtension(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        if (!extension.isNullOrEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let { return it }
        }
        return DEFAULT_MIME_TYPE
    }

    private fun successResponse(
        mimeType: String,
        inputStream: InputStream,
    ): WebResourceResponse =
        WebResourceResponse(
            mimeType,
            null,
            200,
            "OK",
            corsHeaders(),
            inputStream,
        )

    private fun errorResponse(
        statusCode: Int,
        reasonPhrase: String,
        body: String,
    ): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            statusCode,
            reasonPhrase,
            corsHeaders(),
            ByteArrayInputStream(body.toByteArray()),
        )

    private fun corsHeaders(): Map<String, String> = mapOf("Access-Control-Allow-Origin" to "*")
}
