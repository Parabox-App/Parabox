package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.file.ExtensionFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFilterChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    label: @Composable () -> Unit,
    enabled: Boolean = true,
    trailingIcon: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    withoutLeadingIcon: Boolean = false,
    onClick: () -> Unit
){
    val shapeCorner by animateDpAsState(targetValue = if(selected) 24.dp else 8.dp)
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        leadingIcon = {
            AnimatedVisibility(visible = !withoutLeadingIcon && selected,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Done,
                    contentDescription = "",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        trailingIcon = trailingIcon,
        label = label,
        shape = RoundedCornerShape(shapeCorner),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = containerColor.onColor(),
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(borderColor = Color.Transparent)
    )
}