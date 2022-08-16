package com.ojhdtapp.parabox.ui.util

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.message.GroupInfoState

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun UserProfileDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    userName: String = "",
    avatarUri: String? = null,
    sizeClass: WindowSizeClass,
    onUpdateName: () -> Unit,
    onUpdateAvatar: () -> Unit,
    onDismiss: () -> Unit
) {
    if (openDialog) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
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
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SmallTopAppBar(title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "close"
                                )
                            }
                        })
                    NormalPreference(title = userName, leadingIcon = {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarUri?.let { Uri.parse(it) } ?: R.drawable.avatar)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                    .build(),
                                contentDescription = "avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            )
                            Surface(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.BottomEnd),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                elevation = 2.dp
                            ) {
                                Icon(
                                    modifier = Modifier.padding(3.dp),
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "edit avatar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.NavigateNext,
                                contentDescription = "next"
                            )
                        },
                        onClick = {
                            onUpdateName()
                        },
                        onLeadingIconClick = {
                            onUpdateAvatar()
                        }
                    )
                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    PreferencesCategory(text = "已连接服务")
                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    PreferencesCategory(text = "扩展")
                }
            }
        }
    }
}