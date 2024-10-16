package com.ojhdtapp.parabox.ui.navigation

import android.os.Parcelable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

interface RootComponent: BackHandlerOwner {
    val rootStack: Value<ChildStack<*, RootChild>>

    sealed class RootChild {
        class Menu(val component: MenuComponent) : RootChild()
        class Setting(val component: SettingComponent) : RootChild()
    }
}

class DefaultRootComponent(componentContext: ComponentContext) : RootComponent, ComponentContext by componentContext {
    val rootNav = StackNavigation<RootConfig>()

    @OptIn(InternalSerializationApi::class)
    override val rootStack: Value<ChildStack<*, RootComponent.RootChild>> =
        childStack<_, RootConfig, RootComponent.RootChild>(
            source = rootNav,
            serializer = RootConfig::class.serializer(),
            initialConfiguration = RootConfig.Menu,
            key = "root_stack",
            handleBackButton = true,
            childFactory = { config, childComponentContext ->
                when (config) {
                    RootConfig.Menu -> RootComponent.RootChild.Menu(DefaultMenuComponent(childComponentContext))
                    RootConfig.Setting -> RootComponent.RootChild.Setting(DefaultSettingComponent(childComponentContext))
                }
            }
        )

    @Parcelize
    @Serializable
    sealed interface RootConfig : Parcelable {
        @Parcelize
        @Serializable
        data object Menu : RootConfig

        @Parcelize
        @Serializable
        data object Setting : RootConfig
    }
}