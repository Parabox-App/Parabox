package com.ojhdtapp.parabox.core.util

import java.text.DateFormat
import java.text.SimpleDateFormat
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

fun Long.toDescriptiveTime():String{
    val dateString = when(System.currentTimeMillis() - this){
        in 0 until 86400000 -> "今天"
        in 86400000 until 172800000 -> "昨天"
        in 172800000 until 259200000 -> "前天"
        in 259200000 until  604800000 -> when(SimpleDateFormat("F").format(Date(this))){
            "1" -> "星期一"
            "2" -> "星期二"
            "3" -> "星期三"
            "4" -> "星期四"
            "5" -> "星期五"
            "6" -> "星期六"
            "7" -> "星期日"
            else -> SimpleDateFormat("M'月'd'日'").format(Date(this))
        }
        in 604800000 until 31536000000 -> SimpleDateFormat("M'月'd'日'").format(Date(this))
        else -> SimpleDateFormat("yyyy'年'M'月'd'日'").format(Date(this))
    }
    val timeString = SimpleDateFormat("H:m").format(Date(this))
    return "$dateString $timeString"
}

fun Long.toFormattedDate(): String{
    return DateFormat.getDateInstance().format(Date(this))
}