package com.ojhdtapp.parabox.ui.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.common.CommonAvatar

@Composable
fun ContactItem(
    modifier: Modifier = Modifier,
    name: String,
    avatarModel: Any?,
    extName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                CommonAvatar(model = avatarModel, name = name)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = extName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyContactItem(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.placeholder(
                    visible = true,
                    MaterialTheme.colorScheme.secondaryContainer,
                    highlight = PlaceholderHighlight.fade(),
                ),
                text = context.getString(R.string.contact_name),
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.placeholder(
                    visible = true,
                    MaterialTheme.colorScheme.secondaryContainer,
                    highlight = PlaceholderHighlight.fade(),
                ),
                text = context.getString(R.string.contact_name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}