package com.ojhdtapp.parabox.ui.message.chat

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.ui.common.MyFilterChip
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingLayoutType
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.TransformImageView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoArea(
    modifier: Modifier = Modifier,
    contactPagingDataFlow: Flow<PagingData<ContactWithExtensionInfo>>,
    infoAreaState: MessagePageState.InfoAreaState,
    previewerState: ImagePreviewerState,
    imageSnapshotList: List<MessagePageState.ImagePreviewerState.ImagePreviewerItem>,
    onImageClick: (indexOfSnapshot: Int, elementId: Long) -> Unit,
    onLazyGridEndReached: () -> Unit,
    onShowContactDetail: (contactWithExtensionInfo: ContactWithExtensionInfo) -> Unit,
    onEvent: (MessagePageEvent) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Column(modifier = modifier) {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = { Text(text = "会话信息") },
            actions = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text("编辑会话信息")
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { onEvent(MessagePageEvent.OpenInfoArea(false)) }) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "edit")
                    }
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text("关闭")
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { onEvent(MessagePageEvent.OpenInfoArea(false)) }) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "close")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        val tabList = listOf<String>(
            "设置",
            "媒体",
            "文件",
            "成员"
        )
        val pagerState = rememberPagerState() { tabList.size }
        androidx.compose.material3.PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent
        ) {
            tabList.forEachIndexed { index, s ->
                Tab(selected = index == pagerState.currentPage, onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }, text = { Text(text = s) })
            }
        }
        HorizontalPager(modifier = Modifier.weight(1f), state = pagerState) {
            when (it) {
                0 -> {
                    InfoSettingArea(
                        infoAreaState = infoAreaState,
                        onEvent = onEvent
                    )
                }

                1 -> {
                    InfoGalleryArea(
                        imageSnapshotList = imageSnapshotList,
                        previewerState = previewerState,
                        onImageClick = onImageClick,
                        onLazyGridEndReached = onLazyGridEndReached,
                        onEvent = onEvent
                    )
                }

                2 -> {

                }

                3 -> {
                    InfoContactArea(
                        contactPagingDataFlow = contactPagingDataFlow,
                        onShowContactDetail = onShowContactDetail,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoSettingArea(
    modifier: Modifier = Modifier,
    infoAreaState: MessagePageState.InfoAreaState,
    onEvent: (MessagePageEvent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        SettingHeader(text = "通知")
        SettingItem(
            title = "允许通知",
            subTitle = "接收新消息通知",
            selected = false,
            layoutType = SettingLayoutType.NORMAL,
            trailingIcon = {
                Switch(
                    enabled = infoAreaState.realTimeChat != null,
                    checked = infoAreaState.realTimeChat?.isNotificationEnabled ?: false,
                    onCheckedChange = {
                        if (infoAreaState.realTimeChat != null) {
                            onEvent(
                                MessagePageEvent.UpdateChatNotificationEnabled(
                                    infoAreaState.realTimeChat.chatId,
                                    it,
                                    infoAreaState.realTimeChat.isNotificationEnabled
                                )
                            )
                        }
                    })
            }) {
            if (infoAreaState.realTimeChat != null) {
                onEvent(
                    MessagePageEvent.UpdateChatNotificationEnabled(
                        infoAreaState.realTimeChat.chatId,
                        !infoAreaState.realTimeChat.isNotificationEnabled,
                        infoAreaState.realTimeChat.isNotificationEnabled
                    )
                )
            }
        }
        SettingHeader(text = "效率操作")
        SettingItem(
            title = "归档",
            subTitle = "将会话收入归档",
            selected = false,
            layoutType = SettingLayoutType.NORMAL,
            trailingIcon = {
                Switch(
                    enabled = infoAreaState.realTimeChat != null,
                    checked = infoAreaState.realTimeChat?.isArchived ?: false,
                    onCheckedChange = {
                        if (infoAreaState.realTimeChat != null) {
                            onEvent(
                                MessagePageEvent.UpdateChatArchive(
                                    infoAreaState.realTimeChat.chatId,
                                    it,
                                    infoAreaState.realTimeChat.isArchived
                                )
                            )
                        }
                    })
            }) {
            if (infoAreaState.realTimeChat != null) {
                onEvent(
                    MessagePageEvent.UpdateChatArchive(
                        infoAreaState.realTimeChat.chatId,
                        !infoAreaState.realTimeChat.isArchived,
                        infoAreaState.realTimeChat.isArchived
                    )
                )
            }
        }
        SettingItem(
            title = "置顶",
            subTitle = "将会话置于列表顶部",
            selected = false,
            layoutType = SettingLayoutType.NORMAL,
            trailingIcon = {
                Switch(
                    enabled = infoAreaState.realTimeChat != null,
                    checked = infoAreaState.realTimeChat?.isPinned ?: false,
                    onCheckedChange = {
                        if (infoAreaState.realTimeChat != null) {
                            onEvent(
                                MessagePageEvent.UpdateChatPin(
                                    infoAreaState.realTimeChat.chatId,
                                    it,
                                    infoAreaState.realTimeChat.isPinned
                                )
                            )
                        }
                    })
            }) {
            if (infoAreaState.realTimeChat != null) {
                onEvent(
                    MessagePageEvent.UpdateChatPin(
                        infoAreaState.realTimeChat.chatId,
                        !infoAreaState.realTimeChat.isPinned,
                        infoAreaState.realTimeChat.isPinned
                    )
                )
            }
        }
        SettingItem(
            title = "暂时隐藏",
            subTitle = "将会话从列表隐藏，直至收到新消息",
            selected = false,
            layoutType = SettingLayoutType.NORMAL,
            trailingIcon = {
                Switch(
                    enabled = infoAreaState.realTimeChat != null,
                    checked = infoAreaState.realTimeChat?.isHidden ?: false,
                    onCheckedChange = {
                        if (infoAreaState.realTimeChat != null) {
                            onEvent(
                                MessagePageEvent.UpdateChatHide(
                                    infoAreaState.realTimeChat.chatId,
                                    it,
                                    infoAreaState.realTimeChat.isHidden
                                )
                            )
                        }
                    })
            }) {
            if (infoAreaState.realTimeChat != null) {
                onEvent(
                    MessagePageEvent.UpdateChatHide(
                        infoAreaState.realTimeChat.chatId,
                        !infoAreaState.realTimeChat.isHidden,
                        infoAreaState.realTimeChat.isHidden
                    )
                )
            }
        }
        SettingHeader(text = "标签")
        if (infoAreaState.realTimeChat?.tags?.isNotEmpty() == true) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                infoAreaState.realTimeChat?.tags?.forEach {
                    MyFilterChip(
                        selected = false,
                        label = { Text(text = it) }
                    ) {}
                }
            }
        }
        SettingItem(
            title = "编辑标签",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.NewLabel,
                    contentDescription = "new label",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            selected = false,
            layoutType = SettingLayoutType.NORMAL,
        ) {
            if (infoAreaState.realTimeChat != null) {
                onEvent(
                    MessagePageEvent.UpdateEditingChatTags(
                        ChatTagsUpdate(
                            infoAreaState.realTimeChat.chatId,
                            infoAreaState.realTimeChat.tags
                        )

                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        SettingHeader(text = "基础信息")
        Spacer(modifier = Modifier.height(16.dp))
        infoAreaState.realTimeChat?.chatId?.let {
            InfoBlank(key = "ID", value = it.toString())
        }
        infoAreaState.realTimeChat?.uid?.let {
            InfoBlank(key = "UID", value = it)
        }
        InfoBlank(
            key = "子会话ID",
            value = (infoAreaState.realTimeChat?.subChatIds?.takeIf { it.isNotEmpty() }
                ?: listOf("空")).fastJoinToString(",")
        )
        infoAreaState.realTimeChat?.pkg?.let {
            InfoBlank(key = "扩展包名", value = it)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InfoGalleryArea(
    modifier: Modifier = Modifier,
    imageSnapshotList: List<MessagePageState.ImagePreviewerState.ImagePreviewerItem>,
    previewerState: ImagePreviewerState,
    onImageClick: (indexOfSnapshot: Int, elementId: Long) -> Unit,
    onLazyGridEndReached: () -> Unit,
    onEvent: (MessagePageEvent) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val state = rememberLazyGridState()
    LaunchedEffect(state.canScrollForward) {
        if (!state.canScrollForward) {
            Log.d("parabox", "onLazyGridEndReached")
            onLazyGridEndReached()
        }
    }
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        state = state,
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        itemsIndexed(items = imageSnapshotList, key = { index, item -> item.elementId }) { index, item ->
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(item.image.resourceInfo.getModel())
                    .size(coil.size.Size.ORIGINAL)
                    .crossfade(true)
                    .memoryCacheKey("${item.elementId}")
                    .build(),
                error = painterResource(id = R.drawable.image_lost),
                fallback = painterResource(id = R.drawable.image_lost),
                contentScale = ContentScale.Fit,
            )
            TransformImageView(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(1F)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onImageClick(index, item.elementId)
                        }
                    }.animateItem(),
                key = "info_area_${item.elementId}",
                painter = painter,
                previewerState = previewerState,
            )
        }

    }
}

@Composable
fun InfoContactArea(
    modifier: Modifier = Modifier,
    contactPagingDataFlow: Flow<PagingData<ContactWithExtensionInfo>>,
    onShowContactDetail: (contactWithExtensionInfo: ContactWithExtensionInfo) -> Unit,
    onEvent: (MessagePageEvent) -> Unit
) {
    val contactLazyPagingItems = contactPagingDataFlow.collectAsLazyPagingItems()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            count = contactLazyPagingItems.itemCount,
            key = contactLazyPagingItems.itemKey { it.contact.contactId }
        ) { index ->
            val item = contactLazyPagingItems[index]
            if (item == null) {
                EmptyPlainContactItem(
                    modifier = Modifier.animateItem()
                )
            } else {
                PlainContactItem(
                    modifier = Modifier.animateItem(),
                    name = item.contact.name,
                    lastName = (index - 1).takeIf { it >= 0 }
                        ?.let { contactLazyPagingItems.peek(it) }?.contact?.name,
                    avatarModel = item.contact.avatar.getModel(),
                    extName = item.extensionInfo.alias,
                ) {
                    onShowContactDetail(item)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun InfoBlank(
    modifier: Modifier = Modifier,
    key: String,
    value: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            modifier = Modifier.width(108.dp),
            text = key, maxLines = 1, color = MaterialTheme.colorScheme.primary
        )
        Text(modifier = Modifier.weight(1f), text = value, maxLines = 2, color = MaterialTheme.colorScheme.onSurface)
    }
}