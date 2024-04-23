package com.ojhdtapp.paraboxdevelopmentkit.model.init_actions

import android.os.Bundle
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

sealed interface ParaboxInitAction {
    val key: String
    val title: String
    val errMsg: String

    data class InfoAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        val description: String,
        val onResult: suspend () -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    data class TextInputAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        val description: String,
        val label: String,
        val onResult: suspend (value: String) -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    data class SelectAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        val description: String,
        val options: List<String>,
        val onResult: suspend (selectedIndex: Int) -> ParaboxInitActionResult,
    ) : ParaboxInitAction

    data class TextInputWithImageAction(
        override val key: String,
        override val title: String,
        override val errMsg: String,
        val description: String,
        val image: ParaboxResourceInfo,
        val onResult: suspend (value: String) -> ParaboxInitActionResult,
    ) : ParaboxInitAction
}

sealed interface ParaboxInitActionResult {
    data object Done: ParaboxInitActionResult
    class Error (val message: String): ParaboxInitActionResult
}