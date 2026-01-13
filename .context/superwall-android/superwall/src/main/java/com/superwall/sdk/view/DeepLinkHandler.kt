package com.superwall.sdk.view

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.superwall.sdk.Superwall

class DeepLinkHandler : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
        val uri = Uri.parse(data?.toString())
        Superwall.handleDeepLink(uri)
        finish()
    }
}
