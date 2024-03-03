package com.ojhdtapp.parabox.ui.navigation

import android.os.Parcelable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

interface RootComponent {
    val menuStack: Value<ChildStack<*, Child>>

    sealed class Child {
        class Message(val component: MessageComponent) : Child()
        class File(val component: FileComponent) : Child()
        class Contact(val component: ContactComponent) : Child()
    }
}

class DefaultRootComponent(componentContext: ComponentContext) : RootComponent, ComponentContext by componentContext {
    val menuNav = StackNavigation<Config>()

    @OptIn(InternalSerializationApi::class)
    override val menuStack: Value<ChildStack<*, RootComponent.Child>> =
        childStack<Config, RootComponent.Child>(
            source = menuNav,
            serializer = Config::class.serializer(),
            initialConfiguration = Config.Message,
            key = "menu_stack",
            handleBackButton = true,
            childFactory = { config, childComponentContext ->
                when (config) {
                    Config.Message -> RootComponent.Child.Message(MessageComponent(childComponentContext))
                    Config.File -> RootComponent.Child.File(FileComponent(childComponentContext))
                    Config.Contact -> RootComponent.Child.Contact(ContactComponent(childComponentContext))
                }
            }
        )

    @Parcelize
    @Serializable
    sealed interface Config : Parcelable {
        @Parcelize
        @Serializable
        data object Message : Config

        @Parcelize
        @Serializable
        data object File : Config

        @Parcelize
        @Serializable
        data object Contact : Config
    }
}