package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            Column() {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Localized description")
                    }
                    TextButton(
                        onClick = {
                            onConfirm(
                                dateRangePickerState.selectedStartDateMillis!!,
                                dateRangePickerState.selectedEndDateMillis!!
                            )
                        },
                        enabled = confirmEnabled.value
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                }

                DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
            }
        }
    }
}