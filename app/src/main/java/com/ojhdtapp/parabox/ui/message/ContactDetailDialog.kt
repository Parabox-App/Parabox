package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.BlurTransformation
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel
import com.ojhdtapp.parabox.ui.contact.EmptyRelativeChatItem
import com.ojhdtapp.parabox.ui.contact.RelativeChatItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailDialog(
    modifier: Modifier = Modifier,
    contactDetailDialogState: MainSharedState.ContactDetailDialogState,
    onEvent: (MainSharedEvent) -> Unit,
) {
    if (contactDetailDialogState.contactWithExtensionInfo != null) {
        BasicAlertDialog(
            onDismissRequest = {
                onEvent(MainSharedEvent.DismissContactDetailDialog)
            },
            modifier = modifier,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true
            ),
        ) {
            Surface(
                shape = AlertDialogDefaults.shape
            ) {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .offset { IntOffset(0, -scrollState.value / 3) },
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(contactDetailDialogState.contactWithExtensionInfo.contact.avatar.getModel())
                            .transformations(BlurTransformation(LocalContext.current))
                            .build(),
                        contentDescription = "avatar_bg", contentScale = ContentScale.Crop,
                    )
                    Column(
                        modifier = Modifier.verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp), contentAlignment = Alignment.BottomCenter
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 28.dp,
                                    topEnd = 28.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                ),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(49.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(96.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CommonAvatar(
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
                                    model = CommonAvatarModel(
                                        model = contactDetailDialogState.contactWithExtensionInfo.contact.avatar.getModel(),
                                        name = contactDetailDialogState.contactWithExtensionInfo.contact.name
                                    )
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier.padding(16.dp),
                                text = contactDetailDialogState.contactWithExtensionInfo.contact.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1
                            )
                            Row {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        PlainTooltip {
                                            Text("发起会话")
                                        }
                                    },
                                    state = rememberTooltipState()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        onClick = {}
                                    ) {
                                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Message,
                                                contentDescription = "message"
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    tooltip = {
                                        PlainTooltip {
                                            Text("添加至联系人")
                                        }
                                    },
                                    state = rememberTooltipState()
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        onClick = {}
                                    ) {
                                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.PersonAddAlt,
                                                contentDescription = "add_contact"
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        modifier = Modifier.padding(16.dp),
                                        imageVector = Icons.Outlined.LibraryAdd,
                                        contentDescription = "extension source"
                                    )
                                    Text(text = "源信息", style = MaterialTheme.typography.titleMedium)
                                }
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = contactDetailDialogState.contactWithExtensionInfo.extensionInfo.name,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = contactDetailDialogState.contactWithExtensionInfo.extensionInfo.pkg,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            text = contactDetailDialogState.contactWithExtensionInfo.extensionInfo.alias,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        modifier = Modifier.padding(16.dp),
                                        imageVector = Icons.Outlined.Groups,
                                        contentDescription = "extension source"
                                    )
                                    Text(text = "加入的群组", style = MaterialTheme.typography.titleMedium)
                                }

                                when (contactDetailDialogState.loadState) {
                                    LoadState.LOADING -> {
                                        repeat(2) {
                                            EmptyRelativeChatItem()
                                        }
                                    }

                                    LoadState.ERROR -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp), contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "加载出错，请稍后再试")
                                        }
                                    }

                                    LoadState.SUCCESS -> {
                                        Column {
                                            contactDetailDialogState.relativeChatList.forEach {
                                                RelativeChatItem(chat = it) {

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

    }
}