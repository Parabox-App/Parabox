package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.ui.theme.fontSize

@Composable
fun PreferencesCategory(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
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
    val textColor by animateColorAsState(targetValue = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline)
    Surface() {
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
                            color = textColor,
                        )
                    }
                } else {
                    subtitleOff?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(48.dp))
            Switch(checked = checked, onCheckedChange = {
                onCheckedChange(it)
                checked = !checked
            }, enabled = enabled,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                })
        }
    }
}

//@Composable
//fun NormalPreference(
//    modifier: Modifier = Modifier,
//    leadingIcon: (@Composable () -> Unit)? = null,
//    trailingIcon: (@Composable () -> Unit)? = null,
//    title: String,
//    subtitle: String? = null,
//    warning: Boolean = false,
//    horizontalPadding: Dp = 24.dp,
//    onClick: () -> Unit
//) {
//    Row(
//        modifier = modifier
//            .clickable { onClick() }
//            .padding(horizontalPadding, 16.dp)
//            .fillMaxWidth(),
//        horizontalArrangement = Arrangement.Start,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        val textColor =
//            if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
//        if (leadingIcon != null) {
//            Box(modifier = Modifier.padding(end = 16.dp), contentAlignment = Alignment.Center) {
//                leadingIcon()
//            }
//        }
//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = title,
//                style = MaterialTheme.typography.titleLarge,
//                color = textColor,
//                fontSize = MaterialTheme.fontSize.title
//            )
//            subtitle?.let {
//                Spacer(modifier = Modifier.height(4.dp))
//                Text(
//                    text = it,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = textColor
//                )
//            }
//        }
//        if (trailingIcon != null) {
//            Box(modifier = Modifier.padding(start = 16.dp), contentAlignment = Alignment.Center) {
//                trailingIcon()
//            }
//        }
//    }
//}

@Composable
fun NormalPreference(
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    warning: Boolean = false,
    roundedCorner: Boolean = false,
    horizontalPadding: Dp = 24.dp,
    onLeadingIconClick: (() -> Unit)? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
    val titleTextColor by animateColorAsState(
        targetValue = when {
            warning -> MaterialTheme.colorScheme.error
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
    val subTitleTextColor by animateColorAsState(
        targetValue = when {
            warning -> MaterialTheme.colorScheme.error
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(if (roundedCorner) 24.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (onLeadingIconClick != null && leadingIcon != null) {
                Row(
                    modifier = Modifier
                        .clickable { onLeadingIconClick() }
                        .padding(start = horizontalPadding, top = 16.dp, bottom = 16.dp)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(15.dp))
                    Divider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clickable { onClick() }
                    .padding(
                        start = if (onLeadingIconClick != null && leadingIcon != null) 16.dp else horizontalPadding,
                        end = if (onTrailingIconClick != null && trailingIcon != null) 16.dp else horizontalPadding,
                        top = 16.dp,
                        bottom = 16.dp
                    )
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null && onLeadingIconClick == null) {
                    Box(
                        modifier = Modifier.padding(end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leadingIcon()
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = titleTextColor,
                        fontSize = MaterialTheme.fontSize.title
                    )
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = subTitleTextColor
                        )
                    }
                }
                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.padding(start = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingIcon()
                    }
                }
            }
            if (onTrailingIconClick != null && trailingIcon != null) {
                Row(
                    modifier = Modifier
                        .clickable { onTrailingIconClick() }
                        .padding(end = horizontalPadding, top = 16.dp, bottom = 16.dp)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                    Spacer(modifier = Modifier.width(15.dp))
                    trailingIcon()
                }
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
        RoundedCornerDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
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