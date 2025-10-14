package com.superwall.sdk.paywall.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

internal class SuperwallStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

internal class ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViewStorageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViewStorageViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
