package com.ojhdtapp.parabox.ui.setting

import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil
import com.ojhdtapp.parabox.domain.model.Contact

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContactListDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    contactList: List<Contact>,
    contactCheck: (Contact) -> Boolean,
    onValueChange: (target: Contact, value: Boolean) -> Unit,
    loading: Boolean,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            val horizontalPadding = when (sizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 16.dp
                WindowWidthSizeClass.Medium -> 32.dp
                WindowWidthSizeClass.Expanded -> 0.dp
                else -> 16.dp
            }
            Surface(
                modifier = modifier
                    .widthIn(0.dp, 580.dp)
                    .padding(horizontal = horizontalPadding)
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.contact_list),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(0.dp, 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(176.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else {
                            item {
                                if (contactList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(176.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = stringResource(R.string.no_contact))
                                    }
                                }
                            }
                            items(items = contactList, key = { it.contactId }) {
                                MultiSelectItem(
                                    contact = it,
                                    onValueChange = onValueChange,
                                    contactCheck = contactCheck
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiSelectItem(
    modifier: Modifier = Modifier,
    contact: Contact,
    contactCheck: (Contact) -> Boolean,
    onValueChange: (target: Contact, value: Boolean) -> Unit
) {
    Row(modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable {
            onValueChange(contact, !contactCheck(contact))
        }, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(12.dp))
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(
                    AvatarUtil.getAvatar(
                        uri = contact.profile.avatarUri?.let{ Uri.parse(it)},
                        url = contact.profile.avatar,
                        name = contact.profile.name,
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimary,
                    )
                )
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                .build(),
            contentDescription = "avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = contact.profile.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Checkbox(checked = contactCheck(contact), onCheckedChange = { onValueChange(contact, it) })
    }
}