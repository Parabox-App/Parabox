package com.ojhdtapp.paraboxdevelopmentkit.model.init_actions

import android.os.Bundle
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

sealed interface ParaboxInitAction {
    val key: String
    val title: String
    val errMsg: String
    val description: String

    data class InfoAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        override val description: String,
        val onResult: suspend () -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    data class TextInputAction(
        override val key: String,
        override val title: String,
        override val errMsg: String = "",
        override val description: String = "",
        val label: String,
        val type: KeyboardType = KeyboardType.TEXT,
        val defaultValue: String? = null,
        val onResult: suspend (value: String) -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    data class SelectAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        override val description: String,
        val options: List<String>,
        val defaultValue: Int? = null,
        val onResult: suspend (selectedIndex: Int) -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    data class TextInputWithImageAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        override val description: String,
        val image: ParaboxResourceInfo,
        val label: String,
        val type: KeyboardType = KeyboardType.TEXT,
        val defaultValue: String? = null,
        val onResult: suspend (value: String) -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    data class SwitchAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        override val description: String,
        val defaultValue: Boolean,
        val onResult: suspend (isChecked: Boolean) -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    enum class KeyboardType {
        TEXT,
        NUMBER,
        EMAIL,
        PASSWORD
    }
}

sealed interface ParaboxInitActionResult {
    data object Done: ParaboxInitActionResult
    class Error(val message: String): ParaboxInitActionResult
}