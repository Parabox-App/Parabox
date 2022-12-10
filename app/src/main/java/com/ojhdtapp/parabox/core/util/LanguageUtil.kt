package com.ojhdtapp.parabox.core.util

object LanguageUtil {
    fun languageTagMapper(languageTag: String): String {
        return when (languageTag) {
            "rcn" -> "zh"
            else -> languageTag
        }
    }
}