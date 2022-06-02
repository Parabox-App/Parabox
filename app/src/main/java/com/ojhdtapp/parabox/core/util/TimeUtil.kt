package com.ojhdtapp.parabox.core.util

import java.text.DateFormat
import java.util.*

fun Long.toTimeUntilNow(): String {
    return when (val millsUtilNow = System.currentTimeMillis() - this) {
        in 0 until 120000 -> "刚刚"
        in 120000 until 1500000 -> "${millsUtilNow / 60000}分钟前"
        in 1500000 until 3000000 -> "半小时前"
        in 3000000 until  3600000 -> "1小时前"
        in 3600000 until 86400000 -> "${millsUtilNow / 3600000}小时前"
        in 86400000 until 259200000 -> "${millsUtilNow / 86400000}天前"
        else -> this.toFormattedDate()
    }
}

fun Long.toFormattedDate(): String{
    return DateFormat.getDateInstance().format(Date(this))
}