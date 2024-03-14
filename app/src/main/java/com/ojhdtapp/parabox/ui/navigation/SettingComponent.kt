package com.ojhdtapp.parabox.ui.navigation

import android.os.Parcelable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.ojhdtapp.parabox.ui.navigation.settings.ApperaranceSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.ExperimentalSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.ExtensionAddSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.ExtensionSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.GeneralSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.HelpAndSupportSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.LabelDetailSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.LabelSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.NotificationSettingComponent
import com.ojhdtapp.parabox.ui.navigation.settings.StorageSettingComponent
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

interface SettingComponent: BackHandlerOwner {
    val settingStack: Value<ChildStack<*, SettingChild>>
    val settingNav: StackNavigation<DefaultSettingComponent.SettingConfig>

    sealed class SettingChild {
        class GeneralSetting(val component: GeneralSettingComponent) : SettingChild()
        class ExtensionSetting(val component: ExtensionSettingComponent) : SettingChild()
        class ExtensionAddSetting(val component: ExtensionAddSettingComponent) : SettingChild()
        class LabelSetting(val component: LabelSettingComponent) : SettingChild()
        class LabelDetailSetting(val component: LabelDetailSettingComponent) : SettingChild()
        class AppearanceSetting(val component: ApperaranceSettingComponent) : SettingChild()
        class NotificationSetting(val component: NotificationSettingComponent) : SettingChild()
        class StorageSetting(val component: StorageSettingComponent) : SettingChild()
        class ExperimentalSetting(val component: ExperimentalSettingComponent) : SettingChild()
        class HelpAndSupportSetting(val component: HelpAndSupportSettingComponent) : SettingChild()
    }
}

class DefaultSettingComponent(
    componentContext: ComponentContext
) : SettingComponent, ComponentContext by componentContext {
    override val settingNav: StackNavigation<SettingConfig> = StackNavigation<SettingConfig>()

    @OptIn(InternalSerializationApi::class)
    override val settingStack: Value<ChildStack<*, SettingComponent.SettingChild>> =
        childStack(
            source = settingNav,
            serializer = SettingConfig::class.serializer(),
            initialConfiguration = SettingConfig.GeneralSetting,
            key = "setting_stack",
            handleBackButton = true,
            childFactory = { config, childComponentContext ->
                when (config) {
                    SettingConfig.GeneralSetting -> SettingComponent.SettingChild.GeneralSetting(
                        GeneralSettingComponent(
                            childComponentContext
                        )
                    )

                    SettingConfig.ExtensionSetting -> SettingComponent.SettingChild.ExtensionSetting(
                        ExtensionSettingComponent(childComponentContext)
                    )

                    SettingConfig.ExtensionAddSetting -> SettingComponent.SettingChild.ExtensionAddSetting(
                        ExtensionAddSettingComponent(childComponentContext)
                    )

                    SettingConfig.LabelSetting -> SettingComponent.SettingChild.LabelSetting(
                        LabelSettingComponent(
                            childComponentContext
                        )
                    )

                    SettingConfig.LabelDetailSetting -> SettingComponent.SettingChild.LabelDetailSetting(
                        LabelDetailSettingComponent(childComponentContext)
                    )

                    SettingConfig.AppearanceSetting -> SettingComponent.SettingChild.AppearanceSetting(
                        ApperaranceSettingComponent(childComponentContext)
                    )

                    SettingConfig.NotificationSetting -> SettingComponent.SettingChild.NotificationSetting(
                        NotificationSettingComponent(childComponentContext)
                    )

                    SettingConfig.StorageSetting -> SettingComponent.SettingChild.StorageSetting(
                        StorageSettingComponent(childComponentContext)
                    )

                    SettingConfig.ExperimentalSetting -> SettingComponent.SettingChild.ExperimentalSetting(
                        ExperimentalSettingComponent(childComponentContext)
                    )

                    SettingConfig.HelpAndSupportSetting -> SettingComponent.SettingChild.HelpAndSupportSetting(
                        HelpAndSupportSettingComponent(childComponentContext)
                    )
                }
            }
        )

    sealed interface SettingConfig : Parcelable {
        @Parcelize
        @Serializable
        data object GeneralSetting : SettingConfig

        @Parcelize
        @Serializable
        data object ExtensionSetting : SettingConfig

        @Parcelize
        @Serializable
        data object ExtensionAddSetting : SettingConfig

        @Parcelize
        @Serializable
        data object LabelSetting : SettingConfig

        @Parcelize
        @Serializable
        data object LabelDetailSetting : SettingConfig

        @Parcelize
        @Serializable
        data object AppearanceSetting : SettingConfig

        @Parcelize
        @Serializable
        data object NotificationSetting : SettingConfig

        @Parcelize
        @Serializable
        data object StorageSetting : SettingConfig

        @Parcelize
        @Serializable
        data object ExperimentalSetting : SettingConfig

        @Parcelize
        @Serializable
        data object HelpAndSupportSetting : SettingConfig
    }
}