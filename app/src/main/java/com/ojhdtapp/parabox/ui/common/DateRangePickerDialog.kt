package com.ojhdtapp.parabox.ui.common

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ojhdtapp.parabox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    onConfirm: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (openDialog) {
        val dateRangePickerState = rememberDateRangePickerState()
        val confirmEnabled =
            remember { derivedStateOf { dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null } }
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(
                            dateRangePickerState.selectedStartDateMillis!!,
                            dateRangePickerState.selectedEndDateMillis!!
                        )
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text(stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss
                    }
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }) {
            DateRangePicker(state = dateRangePickerState)
        }
    }
}