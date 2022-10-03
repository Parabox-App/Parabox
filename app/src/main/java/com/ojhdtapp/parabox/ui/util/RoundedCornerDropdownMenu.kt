package com.ojhdtapp.parabox.ui.util

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

@Composable
fun RoundedCornerDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    roundedCorner: Dp = 8.dp,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) = MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(roundedCorner))) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        properties = properties,
        content = content
    )
}