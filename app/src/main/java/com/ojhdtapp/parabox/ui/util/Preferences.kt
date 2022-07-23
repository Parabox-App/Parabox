package com.ojhdtapp.parabox.ui.util

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.ui.theme.fontSize

@Composable
fun PreferencesCategory(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier
            .padding(24.dp, 8.dp)
            .fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitleOn: String? = null,
    subtitleOff: String? = null,
    initialChecked: Boolean,
    onCheckedChange: (value: Boolean) -> Unit,
    enabled: Boolean = true,
    horizontalPadding: Dp = 24.dp
) {
    var checked by remember {
        mutableStateOf(initialChecked)
    }
    Row(
        modifier = modifier
            .clickable {
                if (enabled) {
                    checked = !checked
                    onCheckedChange(!initialChecked)
                }
            }
            .padding(horizontalPadding, 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = MaterialTheme.fontSize.title
            )
            if (checked) {

                subtitleOn?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                subtitleOff?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(48.dp))
        Switch(checked = checked, onCheckedChange = {
            onCheckedChange(it)
            checked = !checked
        }, enabled = enabled)
    }
}

@Composable
fun NormalPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(24.dp, 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = MaterialTheme.fontSize.title
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun <T> SimpleMenuPreference(
    modifier: Modifier = Modifier,
    title: String,
    selectedKey: T? = null,
    optionsMap: Map<T, String>,
    onSelect: (selected: T) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier
            .clickable { expanded = true }
            .padding(24.dp, 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for ((key, value) in optionsMap) {
                DropdownMenuItem(text = { Text(text = value) }, onClick = {
                    expanded = false
                    onSelect(key)
                })
            }
        }
        Column() {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = MaterialTheme.fontSize.title
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedKey?.let { optionsMap[it] } ?: optionsMap.values.first(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

        }
    }
}

//@Composable
//fun SwitchPreference(
//    modifier: Modifier = Modifier,
//    checked: Boolean,
//    onCheckedChange: (Boolean) -> Unit,
//    title: String,
//    descriptionOn: String? = null,
//    descriptionOff: String? = null,
//    enabled: Boolean = true,
//    horizontalPadding: Dp = 16.dp
//) {
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .clickable {
//                onCheckedChange(!checked)
//            }.padding(horizontalPadding),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Column(modifier = Modifier.weight(1f)) {
//            Text(text = title, style = MaterialTheme.typography.titleMedium)
//            if (checked) {
//                descriptionOn?.let {
//                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
//                }
//            } else {
//                descriptionOff?.let {
//                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
//                }
//            }
//        }
//        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
//    }
//}