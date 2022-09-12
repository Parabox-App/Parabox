package com.ojhdtapp.parabox.core.util

object FormUtil {
    fun splitPerSpaceOrNewLine(str: String): List<String> = str.split("\\s|([\\r\\n]+)".toRegex())
    fun checkTagMinimumCharacter(str: String): Boolean = str.length >= 2
    fun checkTagMaximumCharacter(str: String): Boolean = str.length < 50
}
fun String.splitKeeping(str: String): List<String> {
    return this.split(str).flatMap { listOf(it, str) }.dropLast(1).filterNot { it.isEmpty() }
}
