package com.ojhdtapp.parabox.core.util

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.google.gson.Gson
import net.sourceforge.pinyin4j.PinyinHelper
import org.json.JSONObject
import java.util.Locale

object FormUtil {
    fun splitPerSpaceOrNewLine(str: String): List<String> = str.split("\\s|([\\r\\n]+)".toRegex())
    fun splitTwoSpacesOrNewLine(str: String): List<String> = str.split("\\s{2}|([\\r\\n]+)".toRegex())
    fun splitNewLine(str: String): List<String> = str.split("[\\r\\n]+".toRegex())
    fun checkTagMinimumCharacter(str: String): Boolean = str.length >= 2
    fun checkTagMaximumCharacter(str: String): Boolean = str.length < 50
    fun getFirstLetter(name: String?): String {
        if (name == null) return "#"
        val firstLetter = name.firstOrNull()?.toString() ?: "#"
        return if (firstLetter.matches(Regex("[a-zA-Z]"))) {
            firstLetter.uppercase(Locale.ROOT)
        } else if (firstLetter.matches(Regex("[\\u4E00-\\u9FA5]"))) {
            val char = firstLetter.toCharArray()
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(char[0])
            pinyinArray?.get(0)?.substring(0, 1)?.uppercase(Locale.ROOT) ?: "#"
        } else {
            "#"
        }
    }
}

fun String.splitKeeping(str: String): List<String> {
    return this.split(str).flatMap { listOf(it, str) }.dropLast(1).filterNot { it.isEmpty() }
}

fun String.ellipsis(maxLength: Int, endKeeping: Int = 3): String {
    return if (this.length <= maxLength) this
    else {
        this.substring(0, maxLength - endKeeping - 1) + "…" + this.substring(this.length - endKeeping, this.length)
    }
}

fun String.getAscllString(): String {
    var sum = ""
    for (i in this.indices) {
        sum += this[i].code.toLong()
    }
    return sum
}

fun JSONObject.optStringOrNull(key: String, fallback: String? = null): String? {
    val obj: Any? = opt(key)
    if (obj is String) {
        return obj
    } else if (obj != null) {
        return obj.toString()
    }
    return fallback
}

fun JSONObject.optBooleanOrNull(key: String, fallback: Boolean? = null): Boolean? {
    val obj: Any? = opt(key)
    when (obj) {
        is Boolean -> return obj
        is Number -> return obj.toInt() == 1
        is String -> return obj.toBooleanStrictOrNull()
    }
    return fallback
}

fun JSONObject.optIntOrNull(key: String, fallback: Int? = null): Int? {
    val obj: Any? = opt(key)
    when (obj) {
        is Number -> return obj.toInt()
        is String -> return obj.toIntOrNull() ?: fallback
    }
    return fallback
}

fun JSONObject.deepCopy() : JSONObject {
    val gson = Gson()
    val jsonStr = gson.toJson(this)
    return gson.fromJson(jsonStr, JSONObject::class.java)
}


@Composable
fun HyperlinkText(
    modifier: Modifier = Modifier,
    fullText: String,
    hyperLinks: Map<String, String>,
    textStyle: TextStyle = TextStyle.Default,
    linkTextColor: Color = Color.Blue,
    linkTextFontWeight: FontWeight = FontWeight.Normal,
    linkTextDecoration: TextDecoration = TextDecoration.None,
    fontSize: TextUnit = TextUnit.Unspecified,
    textColor: Color = Color.Black
) {
    val annotatedString = buildAnnotatedString {
        append(fullText)
        addStyle(
            style = SpanStyle(
                fontSize = fontSize,
                color = textColor
            ),
            start = 0,
            end = fullText.length
        )
        for ((key, value) in hyperLinks) {

            val startIndex = fullText.indexOf(key)
            val endIndex = startIndex + key.length
            addStyle(
                style = SpanStyle(
                    color = linkTextColor,
                    fontSize = fontSize,
                    fontWeight = linkTextFontWeight,
                    textDecoration = linkTextDecoration
                ),
                start = startIndex,
                end = endIndex
            )
            addStringAnnotation(
                tag = "URL",
                annotation = value,
                start = startIndex,
                end = endIndex
            )
        }
    }

    val uriHandler = LocalUriHandler.current

    ClickableText(
        modifier = modifier,
        text = annotatedString,
        style = textStyle,
        onClick = {
            annotatedString
                .getStringAnnotations("URL", it, it)
                .firstOrNull()?.let { stringAnnotation ->
                    uriHandler.openUri(stringAnnotation.item)
                }
        }
    )
}