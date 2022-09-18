package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun SegmentedControl(
    modifier: Modifier = Modifier,
    items: List<String>,
    defaultSelectedItemIndex: Int = 0,
    onItemSelection: (selectedItemIndex: Int) -> Unit
) {
    val selectedIndex = remember { mutableStateOf(defaultSelectedItemIndex) }

    Row(
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            OutlinedButton(
                modifier = when (index) {
                    0 -> {
                        Modifier
                            .wrapContentSize()
                            .offset(0.dp, 0.dp)
                            .zIndex(if (selectedIndex.value == index) 1f else 0f)
                    }
                    else -> {
                        Modifier
                            .wrapContentSize()
                            .offset((-1 * index).dp, 0.dp)
                            .zIndex(if (selectedIndex.value == index) 1f else 0f)
                    }
                },
                onClick = {
                    selectedIndex.value = index
                    onItemSelection(selectedIndex.value)
                },
                shape = when (index) {
                    0 -> RoundedCornerShape(
                        topStartPercent = 50,
                        topEndPercent = 0,
                        bottomStartPercent = 50,
                        bottomEndPercent = 0
                    )
                    items.lastIndex -> RoundedCornerShape(
                        topStartPercent = 0,
                        topEndPercent = 50,
                        bottomStartPercent = 0,
                        bottomEndPercent = 50
                    )
                    else -> RoundedCornerShape(
                        topStartPercent = 0,
                        topEndPercent = 0,
                        bottomStartPercent = 0,
                        bottomEndPercent = 0
                    )
                },
                colors = if (selectedIndex.value == index) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(Color.Transparent)
                },
            ) {
                AnimatedVisibility(
                    visible = selectedIndex.value == index,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 12.dp).size(ButtonDefaults.IconSize),
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "check",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = item,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedIndex.value == index) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}