package com.ojhdtapp.parabox.ui.setting

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
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.ui.theme.fontSize

@Composable
fun SettingItem(
    title: String,
    subTitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = {},
    selected: Boolean,
    layoutType: SettingLayoutType,
    onClick: () -> Unit,
) {
    Surface(
        shape = if (layoutType == SettingLayoutType.SPLIT) RoundedCornerShape(32.dp) else RectangleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(24.dp))
            if (leadingIcon != null) {
                Icon(imageVector = leadingIcon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = MaterialTheme.fontSize.title, color = MaterialTheme.colorScheme.onSurface)
                if (subTitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
    )
}