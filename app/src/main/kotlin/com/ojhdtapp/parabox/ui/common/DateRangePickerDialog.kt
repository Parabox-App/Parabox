package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
        PredictiveBackDialog(
            onDismissRequest = onDismiss,
            modifier = modifier,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .requiredWidth(360.dp)
                    .heightIn(max = 568.dp),
                shape = DatePickerDefaults.shape,
                color = DatePickerDefaults.colors().containerColor,
                tonalElevation = DatePickerDefaults.TonalElevation,
            ) {
                Column(verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(start = 12.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(id = R.string.cancel))
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(id = R.string.cancel))
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
                            Text(stringResource(id = R.string.confirm))
                        }
                    }
                }
            }
        }
    }
}