package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionManager(val context: Context) {
    fun loadExtensions(){
        val extensions = ExtensionLoader.loadExtensions(context)

        _installedExtensionsFlow.value = extensions
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }

    }

    private val _installedExtensionsFlow = MutableStateFlow(emptyList<Extension>())
    val installedExtensionsFlow = _installedExtensionsFlow.asStateFlow()
}