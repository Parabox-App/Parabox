package com.ojhdtapp.paraboxdevelopmentkit.model.init_actions

import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

sealed interface ParaboxInitAction {
    val key: String
    val title: String
    val errMsg: String
    val description: String

    fun withError(message: String): ParaboxInitAction

    data class InfoAction(
        override val key: String,
        override val title: String,
        override val errMsg: String = "",
        override val description: String = "",
    ) : ParaboxInitAction {
        override fun withError(message: String) = copy(errMsg = message)
    }

    data class TextInputAction(
        override val key: String,
        override val title: String,
        override val errMsg: String = "",
        override val description: String = "",
        val label: String,
        val type: KeyboardType = KeyboardType.TEXT,
        val defaultValue: String? = null,
    ) : ParaboxInitAction {
        override fun withError(message: String) = copy(errMsg = message)
    }

    data class SelectAction(
        override val key: String,
        override val title: String,
        override val errMsg: String = "",
        override val description: String = "",
        val options: List<String>,
        val defaultValue: Int? = null,
    ) : ParaboxInitAction {
        override fun withError(message: String) = copy(errMsg = message)
    }

    data class TextInputWithImageAction(
        override val key: String,
        override val title: String,
        override val errMsg: String = "",
        override val description: String = "",
        val image: ParaboxResourceInfo,
        val label: String,
        val type: KeyboardType = KeyboardType.TEXT,
        val defaultValue: String? = null,
    ) : ParaboxInitAction {
        override fun withError(message: String) = copy(errMsg = message)
    }

    data class SwitchAction(
        override val key: String,
        override val title: String,
        override val errMsg: String = "",
        override val description: String = "",
        val defaultValue: Boolean = false,
    ) : ParaboxInitAction {
        override fun withError(message: String) = copy(errMsg = message)
    }

    data class ExternalAuthAction(
        override val key: String,
        override val title: String,
        override val errMsg: String = "",
        override val description: String = "",
        val activityAction: String = "",
        val targetPackage: String = "",
        val authUrl: String = "",
        val buttonLabel: String = "前往授权",
    ) : ParaboxInitAction {
        override fun withError(message: String) = copy(errMsg = message)
    }

    enum class KeyboardType {
        TEXT,
        NUMBER,
        EMAIL,
        PASSWORD
    }
}

sealed interface ParaboxInitActionResult {
    data object Done : ParaboxInitActionResult
    data class Error(val message: String) : ParaboxInitActionResult
}
