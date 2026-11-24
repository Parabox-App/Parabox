package com.ojhdtapp.paraboxdevelopmentkit.model.config_item

import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction.KeyboardType

sealed interface ParaboxConfigItem {
    val key: String
    val title: String
    val description: String

    data class Category(
        override val key: String,
        override val title: String,
        override val description: String = "",
    ) : ParaboxConfigItem

    data class TextInputConfigItem(
        override val key: String,
        override val title: String,
        override val description: String = "",
        val label: String,
        val type: KeyboardType = KeyboardType.TEXT,
        val defaultValue: String? = null,
    ) : ParaboxConfigItem

    data class SelectConfigItem(
        override val key: String,
        override val title: String,
        override val description: String = "",
        val options: List<String>,
        val defaultValue: Int? = null,
    ) : ParaboxConfigItem

    data class SwitchConfigItem(
        override val key: String,
        override val title: String,
        override val description: String = "",
        val defaultValue: Boolean = false,
    ) : ParaboxConfigItem
}
