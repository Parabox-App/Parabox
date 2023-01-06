package com.ojhdtapp.parabox.ui.util

import android.net.Uri
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.ui.setting.AgreementDialog

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
    pluginList: List<AppModel>,
    sizeClass: WindowSizeClass,
    onUpdateName: () -> Unit,
    onUpdateAvatar: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
            var showPrivacyDialog by remember {
                mutableStateOf(false)
            }
            var showTermsDialog by remember {
                mutableStateOf(false)
            }
            AgreementDialog(
                showDialog = showPrivacyDialog,
                icon = {
                    Icon(imageVector = Icons.Outlined.PrivacyTip, contentDescription = "privacy")
                },
                title = stringResource(id = R.string.privacy),
                contentResId = R.string.privacy_content,
                onConfirm = {
                    showPrivacyDialog = false
                },
                onDismiss = {
                    showPrivacyDialog = false
                },
            )
            AgreementDialog(
                showDialog = showTermsDialog,
                icon = {
                    Icon(imageVector = Icons.Outlined.Gavel, contentDescription = "terms")
                },
                title = stringResource(id = R.string.terms),
                contentResId = R.string.terms_content,
                onConfirm = {
                    showTermsDialog = false
                },
                onDismiss = {
                    showTermsDialog = false
                },
            )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    TopAppBar(
                        title = {},
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
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    )
                    NormalPreference(modifier = Modifier.padding(16.dp),title = userName, leadingIcon = {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(
                                        AvatarUtil.getAvatar(
                                            context = context,
                                            uri = avatarUri?.let { Uri.parse(it) },
                                            url = null,
                                            name = null,
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
                        roundedCorner = true,
                        selected = true,
                        onClick = {
                            onUpdateName()
                        },
                        onLeadingIconClick = {
                            onUpdateAvatar()
                        }
                    )
                    if (pluginList.isNotEmpty()) {
                        pluginList.forEach {
                            NormalPreference(
                                title = it.name,
                                subtitle = context.getString(R.string.extension_info, it.version, it.author),
                                leadingIcon = {
                                    AsyncImage(
                                        model = it.icon,
                                        contentDescription = "icon",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(
                                                CircleShape
                                            )
                                    )
                                },
                                trailingIcon = {
                                    when (it.runningStatus) {
                                        AppModel.RUNNING_STATUS_DISABLED -> Icon(
                                            imageVector = Icons.Outlined.Block,
                                            contentDescription = "disabled"
                                        )
                                        AppModel.RUNNING_STATUS_ERROR -> Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = "error",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        AppModel.RUNNING_STATUS_RUNNING -> Icon(
                                            imageVector = Icons.Outlined.CheckCircleOutline,
                                            contentDescription = "running",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        AppModel.RUNNING_STATUS_CHECKING -> CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                onClick = {}
                            )
                        }
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTermsDialog = true }) {
                            Text(
                                text = stringResource(id = R.string.terms),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { showPrivacyDialog = true }) {
                            Text(
                                text = stringResource(id = R.string.privacy),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}