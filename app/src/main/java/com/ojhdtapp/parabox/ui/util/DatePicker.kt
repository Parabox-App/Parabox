package com.ojhdtapp.parabox.ui.util

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.datepicker.MaterialDatePicker
import com.ojhdtapp.parabox.R
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Composable
fun rememberDatePicker(
    initialDate: Date = Date(),
    onDateChange: (newDate: Date) -> Unit
): DatePickerDialog {
    val context = LocalContext.current
    val initialLocalDate = localDateFor(initialDate)
    val initialYear = initialLocalDate.year
    val initialMonth = initialLocalDate.monthValue - 1 // month 5 is june in  java calendar
    val initialDayOfMonth = initialLocalDate.dayOfMonth
    println("Initial year $initialYear month $initialMonth day $initialDayOfMonth")
    val datePickerDialog = DatePickerDialog(
        context,
        R.style.date_picker_theme,
        { _, year: Int, month: Int, dayOfMonth: Int ->
            val newLocalDate = LocalDate.of(year, month + 1, dayOfMonth)
            println("Year $year Month $month Day $dayOfMonth")
            onDateChange(dateFor(newLocalDate))
        },
        initialYear, initialMonth, initialDayOfMonth
    )
    return remember { datePickerDialog }
}

@Composable
fun rememberDateRangePicker(
    initialDate: Date = Date(),
    onDateChange: (newDate: Date) -> Unit
): MaterialDatePicker<androidx.core.util.Pair<Long, Long>> {
    val context = LocalContext.current
    val initialLocalDate = localDateFor(initialDate)
    val initialYear = initialLocalDate.year
    val initialMonth = initialLocalDate.monthValue - 1 // month 5 is june in  java calendar
    val initialDayOfMonth = initialLocalDate.dayOfMonth
//    val datePickerDialog = DatePickerDialog(
//        context,
//        R.style.date_picker_theme,
//        { _, year: Int, month: Int, dayOfMonth: Int ->
//            val newLocalDate = LocalDate.of(year, month + 1, dayOfMonth)
//            onDateChange(dateFor(newLocalDate))
//        },
//        initialYear, initialMonth, initialDayOfMonth
//    )
    val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
        .setTitleText("请选择范围")
        .setSelection(
            androidx.core.util.Pair(
                MaterialDatePicker.thisMonthInUtcMilliseconds(),
                MaterialDatePicker.todayInUtcMilliseconds()
            )
        )
        .build().apply {
            addOnPositiveButtonClickListener {
                // Respond to positive button click.
            }
            addOnNegativeButtonClickListener {
                // Respond to negative button click.
            }
            addOnCancelListener {
                // Respond to cancel button click.
            }
            addOnDismissListener {
                // Respond to dismiss events.
            }
        }
    return remember { dateRangePicker }
}

fun dateFor(ld: LocalDate): Date {
    return Date.from(
        ld.atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
    )
}

fun localDateFor(date: Date): LocalDate {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}