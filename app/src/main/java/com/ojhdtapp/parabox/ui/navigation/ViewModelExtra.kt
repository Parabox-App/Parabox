package com.ojhdtapp.parabox.ui.navigation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.InstanceKeeperOwner
import com.arkivanov.essenty.instancekeeper.getOrCreate

internal fun InstanceKeeperOwner.viewModelStoreOwner(): ViewModelStoreOwner =
    instanceKeeper.getOrCreate(::ViewModelStoreOwnerInstance)

private class ViewModelStoreOwnerInstance : ViewModelStoreOwner, InstanceKeeper.Instance {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    override fun onDestroy() {
        viewModelStore.clear()
    }
}