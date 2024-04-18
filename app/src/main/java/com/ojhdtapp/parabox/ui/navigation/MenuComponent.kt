package com.ojhdtapp.parabox.ui.navigation

import android.os.Parcelable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

interface MenuComponent {
    val menuStack: Value<ChildStack<*, MenuChild>>
    val menuNav : StackNavigation<DefaultMenuComponent.MenuConfig>
    
    sealed class MenuChild {
        class Message(val component: MessageComponent) : MenuChild()
        class File(val component: FileComponent) : MenuChild()
        class Contact(val component: ContactComponent) : MenuChild()
    }
}

class DefaultMenuComponent(
    componentContext: ComponentContext
) : MenuComponent, ComponentContext by componentContext {
    override val menuNav = StackNavigation<MenuConfig>()

    @OptIn(InternalSerializationApi::class)
    override val menuStack: Value<ChildStack<*, MenuComponent.MenuChild>> =
        childStack<_, MenuConfig, MenuComponent.MenuChild>(
            source = menuNav,
            serializer = MenuConfig::class.serializer(),
            initialConfiguration = MenuConfig.Message,
            key = "menu_stack",
            handleBackButton = true,
            childFactory = { config, childComponentContext ->
                when (config) {
                    MenuConfig.Message -> MenuComponent.MenuChild.Message(MessageComponent(childComponentContext))
                    MenuConfig.File -> MenuComponent.MenuChild.File(FileComponent(childComponentContext))
                    MenuConfig.Contact -> MenuComponent.MenuChild.Contact(ContactComponent(childComponentContext))
                }
            }
        )
    @Parcelize
    @Serializable
    sealed interface MenuConfig : Parcelable {
        @Parcelize
        @Serializable
        data object Message : MenuConfig

        @Parcelize
        @Serializable
        data object File : MenuConfig

        @Parcelize
        @Serializable
        data object Contact : MenuConfig
    }
}
