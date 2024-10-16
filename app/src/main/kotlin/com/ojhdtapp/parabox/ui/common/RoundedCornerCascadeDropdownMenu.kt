package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import me.saket.cascade.CascadeColumnScope
import me.saket.cascade.CascadeDropdownMenu
import me.saket.cascade.CascadeState
import me.saket.cascade.rememberCascadeState

@Composable
fun RoundedCornerCascadeDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    fixedWidth: Dp = 196.dp,
    shadowElevation: Dp = 3.dp,
    properties: PopupProperties = PopupProperties(focusable = true),
    state: CascadeState = rememberCascadeState(),
    content: @Composable CascadeColumnScope.() -> Unit
) = MaterialTheme(
    shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.small),
    colorScheme = MaterialTheme.colorScheme.copy(
        surface = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    )
) {
    CascadeDropdownMenu(
        expanded,
        onDismissRequest,
        modifier,
        offset,
        fixedWidth,
        shadowElevation,
        properties,
        state,
        RoundedCornerShape(8.dp),
        content
    )
}