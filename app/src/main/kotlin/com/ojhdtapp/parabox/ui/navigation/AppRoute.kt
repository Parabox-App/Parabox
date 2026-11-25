package com.ojhdtapp.parabox.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.ojhdtapp.parabox.ui.MenuNavigateTarget
import com.ojhdtapp.parabox.ui.SettingNavigateTarget
import com.ojhdtapp.parabox.ui.setting.Setting
import kotlinx.serialization.Serializable

/**
 * Top-level root destinations rendered by the root [androidx.navigation3.ui.NavDisplay].
 */
@Serializable
sealed interface RootRoute : NavKey {
    @Serializable
    data object Main : RootRoute

    @Serializable
    data class Settings(val initialCategory: Setting? = null) : RootRoute
}

/**
 * Main menu destinations rendered inside the adaptive [com.ojhdtapp.parabox.ui.navigation.suite.NavigationSuite].
 * Supports 1-pane, 2-pane (List-Detail), and 3-pane (List-Detail-Extra) via ListDetailSceneStrategy.
 */
@Serializable
sealed interface MenuRoute : NavKey {
    val isTopLevel: Boolean
        get() = false

    val tab: MenuTab

    @Serializable
    data object MessageList : MenuRoute {
        override val isTopLevel: Boolean = true
        override val tab: MenuTab = MenuTab.Message
    }

    @Serializable
    data object ChatDetail : MenuRoute {
        override val tab: MenuTab = MenuTab.Message
    }

    /**
     * Right-side chat info sidebar modeled as ListDetailSceneStrategy.extraPane().
     * Adapts automatically from full-screen predictive-back pane on compact screens
     * to a 3rd side-by-side pane on large tablets and desktop windows.
     */
    @Serializable
    data object ChatInfo : MenuRoute {
        override val tab: MenuTab = MenuTab.Message
    }

    @Serializable
    data object FileList : MenuRoute {
        override val isTopLevel: Boolean = true
        override val tab: MenuTab = MenuTab.File
    }

    @Serializable
    data object ContactList : MenuRoute {
        override val isTopLevel: Boolean = true
        override val tab: MenuTab = MenuTab.Contact
    }

    @Serializable
    data object ContactDetail : MenuRoute {
        override val tab: MenuTab = MenuTab.Contact
    }
}

enum class MenuTab {
    Message,
    File,
    Contact,
}

fun MenuTab.toTopLevelRoute(): MenuRoute = when (this) {
    MenuTab.Message -> MenuRoute.MessageList
    MenuTab.File -> MenuRoute.FileList
    MenuTab.Contact -> MenuRoute.ContactList
}

fun MenuNavigateTarget.toTopLevelRoute(): MenuRoute = when (this) {
    MenuNavigateTarget.Message -> MenuRoute.MessageList
    MenuNavigateTarget.File -> MenuRoute.FileList
    MenuNavigateTarget.Contact -> MenuRoute.ContactList
}

/**
 * Settings destinations rendered by the Settings [androidx.navigation3.ui.NavDisplay]
 * using a single unified back stack + ListDetailSceneStrategy (no nested Decompose conflict).
 */
@Serializable
sealed interface SettingRoute : NavKey {
    val category: Setting?
        get() = null

    @Serializable
    data object Home : SettingRoute

    @Serializable
    data object General : SettingRoute {
        override val category: Setting = Setting.GENERAL
    }

    @Serializable
    data object Extension : SettingRoute {
        override val category: Setting = Setting.ADDONS
    }

    @Serializable
    data object ExtensionAdd : SettingRoute {
        override val category: Setting = Setting.ADDONS
    }

    @Serializable
    data object ExtensionConfig : SettingRoute {
        override val category: Setting = Setting.ADDONS
    }

    @Serializable
    data object Label : SettingRoute {
        override val category: Setting = Setting.LABELS
    }

    @Serializable
    data object LabelDetail : SettingRoute {
        override val category: Setting = Setting.LABELS
    }

    @Serializable
    data object Appearance : SettingRoute {
        override val category: Setting = Setting.APPEARANCE
    }

    @Serializable
    data object Notification : SettingRoute {
        override val category: Setting = Setting.NOTIFICATION
    }

    @Serializable
    data object Storage : SettingRoute {
        override val category: Setting = Setting.STORAGE
    }

    @Serializable
    data object Cloud : SettingRoute {
        override val category: Setting = Setting.CLOUD
    }

    @Serializable
    data object Experimental : SettingRoute {
        override val category: Setting = Setting.EXPERIMENTAL
    }

    @Serializable
    data object Help : SettingRoute {
        override val category: Setting = Setting.HELP
    }

    @Serializable
    data object OpenSourceLicense : SettingRoute {
        override val category: Setting = Setting.HELP
    }
}

fun Setting.toSettingRoute(): SettingRoute = when (this) {
    Setting.GENERAL -> SettingRoute.General
    Setting.ADDONS -> SettingRoute.Extension
    Setting.LABELS -> SettingRoute.Label
    Setting.APPEARANCE -> SettingRoute.Appearance
    Setting.NOTIFICATION -> SettingRoute.Notification
    Setting.STORAGE -> SettingRoute.Storage
    Setting.CLOUD -> SettingRoute.Cloud
    Setting.EXPERIMENTAL -> SettingRoute.Experimental
    Setting.HELP -> SettingRoute.Help
}

fun SettingNavigateTarget.toSetting(): Setting = when (this) {
    SettingNavigateTarget.GENERAL -> Setting.GENERAL
    SettingNavigateTarget.ADDONS -> Setting.ADDONS
    SettingNavigateTarget.LABELS -> Setting.LABELS
    SettingNavigateTarget.APPEARANCE -> Setting.APPEARANCE
    SettingNavigateTarget.NOTIFICATION -> Setting.NOTIFICATION
    SettingNavigateTarget.STORAGE -> Setting.STORAGE
    SettingNavigateTarget.CLOUD -> Setting.CLOUD
    SettingNavigateTarget.EXPERIMENTAL -> Setting.EXPERIMENTAL
    SettingNavigateTarget.HELP -> Setting.HELP
}

object SceneKeys {
    const val MESSAGE = "scene_message"
    const val CONTACT = "scene_contact"
    const val SETTING = "scene_setting"
}
