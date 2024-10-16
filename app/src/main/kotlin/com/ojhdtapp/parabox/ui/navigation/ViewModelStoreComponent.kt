package com.ojhdtapp.parabox.ui.navigation

import android.os.Bundle
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.arkivanov.decompose.ComponentContext

class ViewModelStoreComponent(
    ctx: ComponentContext,
    args: Bundle = Bundle(),
) : ViewModelStoreOwner by ctx.viewModelStoreOwner(),
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val lifecycle: Lifecycle = LifecycleRegistry(provider = this)

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        SavedStateViewModelFactory(application = null, owner = this, defaultArgs = args)

    init {
        savedStateRegistryController.performRestore(ctx.stateKeeper.consume(KEY_SAVED_STATE, BundleSerializer))

        ctx.stateKeeper.register(KEY_SAVED_STATE, BundleSerializer) {
            Bundle().also(savedStateRegistryController::performSave)
        }
    }

    private companion object {
        private const val KEY_SAVED_STATE = "saved_state"
    }
}