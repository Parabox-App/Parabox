package com.ojhdtapp.parabox.core.util

import android.content.Context
import com.ojhdtapp.parabox.R
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

fun Long.toTimeUntilNow(context: Context): String {
    return when (val millsUtilNow = abs(System.currentTimeMillis() - this)) {
        in 0 until 120000 -> context.getString(R.string.just_now)
        in 120000 until 1500000 -> context.getString(R.string.minutes_ago, millsUtilNow / 60000)
        in 1500000 until 3000000 -> context.getString(R.string.half_an_hour_ago)
        in 3000000 until 3600000 -> context.getString(R.string.hour_ago)
        in 3600000 until 86400000 -> context.getString(R.string.hours_ago, millsUtilNow / 3600000)
        in 86400000 until 172800000 -> context.getString(R.string.day_ago)
        in 172800000 until 259200000 -> context.getString(R.string.days_ago, millsUtilNow / 86400000)
        else -> this.toFormattedDate(context)
    }
}

fun Long.toDescriptiveTime(context: Context): String {
    val dateString = if (abs(System.currentTimeMillis() - this) < 518400000) {
        val currentTimeMillis = System.currentTimeMillis()
        val hourInDay =
            SimpleDateFormat("H", Locale.getDefault()).format(Date(currentTimeMillis)).toInt()
        val minuteInHour =
            SimpleDateFormat("m", Locale.getDefault()).format(Date(currentTimeMillis)).toInt()
        val secondInMinute =
            SimpleDateFormat("s", Locale.getDefault()).format(Date(currentTimeMillis)).toInt()
        val startOfTodayInMills =
            currentTimeMillis - hourInDay * 3600000 - minuteInHour * 60000 - secondInMinute * 1000
        when {
            startOfTodayInMills - this <= 0 -> context.getString(R.string.today)
            startOfTodayInMills - this in 0 until 86400000 -> context.getString(R.string.yesterday)
            startOfTodayInMills - this in 86400000 until 172800000 -> context.getString(R.string.the_day_before_yesterday)
            else -> when (SimpleDateFormat("u", Locale.getDefault()).format(Date(this))) {
                "1" -> context.getString(R.string.monday)
                "2" -> context.getString(R.string.tuesday)
                "3" -> context.getString(R.string.wednesday)
                "4" -> context.getString(R.string.thursday)
                "5" -> context.getString(R.string.friday)
                "6" -> context.getString(R.string.saturday)
                "7" -> context.getString(R.string.sunday)
                else -> SimpleDateFormat(context.getString(R.string.date_format_pattern), Locale.getDefault()).format(Date(this))
            }
        }
    } else SimpleDateFormat(context.getString(R.string.date_format_pattern), Locale.getDefault()).format(Date(this))
//    val dateString = when (abs(System.currentTimeMillis() - this)) {
//        in 0 until 86400000 -> "今天"
//        in 86400000 until 172800000 -> "昨天"
//        in 172800000 until 259200000 -> "前天"
//        in 259200000 until 604800000 -> when (SimpleDateFormat("F").format(Date(this))) {
//            "1" -> "星期一"
//            "2" -> "星期二"
//            "3" -> "星期三"
//            "4" -> "星期四"
//            "5" -> "星期五"
//            "6" -> "星期六"
//            "7" -> "星期日"
//            else -> SimpleDateFormat("M'月'd'日'").format(Date(this))
//        }
//        in 604800000 until 31536000000 -> SimpleDateFormat("M'月'd'日'").format(Date(this))
//        else -> SimpleDateFormat("yyyy'年'M'月'd'日'").format(Date(this))
//    }
    val timeString = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(this))
    return "$dateString $timeString"
}

fun Long.toFormattedDate(context: Context): String {
    return SimpleDateFormat(context.getString(R.string.date_format_pattern), Locale.getDefault()).format(Date(this))
}

fun Long.toDateAndTimeString(): String {
    return SimpleDateFormat(
        "yyyy-MM-dd-HH-mm-ss-",
        Locale.getDefault()
    ).format(Date(this)) + this.toString().substring(11)
}

fun Long.toDescriptiveDateAndTime(): String {
    return SimpleDateFormat(
        "MMM d h:mm",
        Locale.getDefault()
    ).format(Date(this))
}

fun Long.toMSString(): String{
    val df = DecimalFormat("#").apply {
        roundingMode = RoundingMode.DOWN
    }
    val totalSecond = (this / 1000).toFloat().roundToInt()
    val minute = df.format(totalSecond / 60)
    val second = totalSecond % 60
    return "${if(totalSecond > 60) minute.plus("′") else ""}${second}“"
}