package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel
import com.ojhdtapp.parabox.ui.common.placeholder

@Composable
fun PlainContactItem(
    modifier: Modifier = Modifier,
    name: String,
    lastName: String?,
    avatarModel: Any?,
    extName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp)) {
                val firstLetter = FormUtil.getFirstLetter(name)
                val lastNameFirstLetter = lastName?.let { FormUtil.getFirstLetter(it) }
                if (lastName == null || firstLetter != lastNameFirstLetter) {
                    Text(
                        text = firstLetter,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            CommonAvatar(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                model = CommonAvatarModel(model = avatarModel, name = name))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(16.dp))
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    text = extName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun EmptyPlainContactItem(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
//                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.placeholder(
                    isLoading = true
                ),
                text = context.getString(R.string.contact_name),
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.placeholder(
                    isLoading = true
                ),
                text = context.getString(R.string.contact_name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}