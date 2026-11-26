package com.ojhdtapp.parabox.ui.common

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAccountPanelSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

object AccountPanelTransitionKeys {
    const val CONTAINER = "account_panel_container"
    const val AVATAR = "account_panel_avatar"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchBarAccountAvatarButton(
    mainSharedState: MainSharedState,
    avatarSize: androidx.compose.ui.unit.Dp = 36.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sharedTransitionScope = LocalAccountPanelSharedTransitionScope.current
    AnimatedVisibility(
        visible = !mainSharedState.openMainDialog,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        modifier = modifier,
    ) {
        val animatedVisibilityScope = this
        val containerMod = if (sharedTransitionScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = AccountPanelTransitionKeys.CONTAINER),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(dampingRatio = 0.85f, stiffness = 380f)
                    },
                    clipInOverlayDuringTransition = OverlayClip(CircleShape),
                )
            }
        } else {
            Modifier
        }
        val avatarMod = if (sharedTransitionScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = AccountPanelTransitionKeys.AVATAR),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(dampingRatio = 0.85f, stiffness = 380f)
                    },
                )
            }
        } else {
            Modifier
        }
        IconButton(
            modifier = containerMod,
            onClick = onClick,
        ) {
            CommonAvatar(
                modifier = avatarMod
                    .clip(CircleShape)
                    .size(avatarSize),
                model = CommonAvatarModel(
                    model = mainSharedState.datastore.localAvatarUri.takeIf { it != Uri.EMPTY },
                    name = mainSharedState.datastore.localName,
                ),
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AccountStatusPanelOverlay(
    mainSharedState: MainSharedState,
    onEvent: (MainSharedEvent) -> Unit,
    onOpenExtensionSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sharedTransitionScope = LocalAccountPanelSharedTransitionScope.current
    val visible = mainSharedState.openMainDialog

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onEvent(MainSharedEvent.PickCustomAvatar(uri))
        }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editingName by remember(mainSharedState.datastore.localName) {
        mutableStateOf(mainSharedState.datastore.localName)
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(text = "修改本地昵称") },
            text = {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    singleLine = true,
                    placeholder = { Text(text = "输入昵称") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editingName.trim().isNotEmpty(),
                    onClick = {
                        onEvent(MainSharedEvent.UpdateLocalName(editingName))
                        showEditNameDialog = false
                    },
                ) {
                    Text(text = "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(text = "取消")
                }
            },
        )
    }

    BackHandler(enabled = visible) {
        onEvent(MainSharedEvent.DismissAccountPanel)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f),
    ) {
        val animatedVisibilityScope = this
        val currentAvatarStr = mainSharedState.datastore.localAvatarUri
            .takeIf { it != Uri.EMPTY }
            ?.toString()
            .orEmpty()
        val connections = mainSharedState.accountPanel.connections
        val activeCount = connections.count { it.status == MainSharedState.ConnectionStatusKind.ACTIVE }
        val summaryText = when {
            connections.isEmpty() -> "暂无已配置的扩展连接"
            else -> "$activeCount / ${connections.size} 个扩展连接在线"
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onEvent(MainSharedEvent.DismissAccountPanel)
                }
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            val containerModifier = if (sharedTransitionScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = AccountPanelTransitionKeys.CONTAINER),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.85f, stiffness = 380f)
                        },
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(28.dp)),
                    )
                }
            } else {
                Modifier
            }

            val avatarSharedModifier = if (sharedTransitionScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = AccountPanelTransitionKeys.AVATAR),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.85f, stiffness = 380f)
                        },
                    )
                }
            } else {
                Modifier
            }

            Surface(
                modifier = containerModifier
                    .widthIn(min = 300.dp, max = 380.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { /* consume clicks inside card */ },
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Top header row: refresh icon + close icon aligned to end
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onEvent(MainSharedEvent.RefreshAccountPanel) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "refresh accounts",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { onEvent(MainSharedEvent.DismissAccountPanel) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hero Avatar (shared element target) with Edit badge rendered above shared overlay
                    val badgeOverlayModifier = if (sharedTransitionScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 2f)
                        }
                    } else {
                        Modifier
                    }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        CommonAvatar(
                            modifier = avatarSharedModifier
                                .size(68.dp)
                                .clip(CircleShape),
                            model = CommonAvatarModel(
                                model = mainSharedState.datastore.localAvatarUri.takeIf { it != Uri.EMPTY },
                                name = mainSharedState.datastore.localName,
                            ),
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                        )
                        Surface(
                            modifier = badgeOverlayModifier
                                .align(Alignment.BottomEnd)
                                .zIndex(2f)
                                .animateEnterExit(
                                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                                    exit = fadeOut() + scaleOut(targetScale = 0.6f),
                                )
                                .size(26.dp)
                                .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "edit avatar",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable local nickname row
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                editingName = mainSharedState.datastore.localName
                                showEditNameDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = mainSharedState.datastore.localName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "edit nickname",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Inner grouped card for active extension connections & accounts
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        if (connections.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "尚未添加扩展连接，点击下方前往配置",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                connections.forEachIndexed { index, item ->
                                    val displayAccountName = item.accountName?.takeIf { it.isNotBlank() }
                                    val isProfileSelected = when {
                                        !item.accountAvatarUri.isNullOrBlank() ->
                                            item.accountAvatarUri == currentAvatarStr &&
                                                (displayAccountName == null || displayAccountName == mainSharedState.datastore.localName)
                                        displayAccountName != null ->
                                            currentAvatarStr.isEmpty() && displayAccountName == mainSharedState.datastore.localName
                                        else -> false
                                    }
                                    ConnectionAccountRow(
                                        item = item,
                                        isProfileSelected = isProfileSelected,
                                        onSelectProfile = {
                                            onEvent(
                                                MainSharedEvent.ApplyConnectionProfile(
                                                    name = displayAccountName ?: item.alias,
                                                    avatarUri = item.accountAvatarUri ?: "",
                                                )
                                            )
                                        },
                                    )
                                    if (index < connections.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Footer: Manage extensions
                    TextButton(
                        onClick = {
                            onEvent(MainSharedEvent.DismissAccountPanel)
                            onOpenExtensionSettings()
                        },
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "管理扩展连接")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionAccountRow(
    item: MainSharedState.ConnectionAccountItem,
    isProfileSelected: Boolean,
    onSelectProfile: () -> Unit,
) {
    val canSelectProfile = !item.accountAvatarUri.isNullOrBlank() || !item.accountName.isNullOrBlank()
    val rowBg = if (isProfileSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val titleColor = if (isProfileSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (isProfileSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val (badgeBg, badgeFg) = when (item.status) {
        MainSharedState.ConnectionStatusKind.ACTIVE ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        MainSharedState.ConnectionStatusKind.CONNECTING,
        MainSharedState.ConnectionStatusKind.RECONNECTING ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        MainSharedState.ConnectionStatusKind.ERROR ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        MainSharedState.ConnectionStatusKind.STOPPED ->
            MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(enabled = canSelectProfile, onClick = onSelectProfile)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CommonAvatar(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            model = CommonAvatarModel(
                model = item.accountAvatarUri,
                name = item.accountName ?: item.alias,
            ),
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = item.accountName ?: item.alias,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        text = item.statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(item.alias)
                    if (item.extensionName.isNotBlank() && item.extensionName != item.alias) {
                        append(" · ")
                        append(item.extensionName)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        when {
            item.isLoadingAccount -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
            isProfileSelected -> {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "selected profile",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
