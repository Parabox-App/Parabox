package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.isGranted
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.theme.fontSize

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    selected: Boolean,
    layoutType: LayoutType,
    disabled: Boolean = false,
    clickableOnly: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.combinedClickable(
            enabled = !disabled,
            onClick = onClick,
            onLongClick = if (!clickableOnly) onLongClick else null
        ),
        shape = if (layoutType == LayoutType.SPLIT) RoundedCornerShape(32.dp) else RectangleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(24.dp))
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = MaterialTheme.fontSize.title, color = if (disabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subTitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (disabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(24.dp))
                trailingIcon()
            }
            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
fun SettingHeader(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
    )
}