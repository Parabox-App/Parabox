package com.ojhdtapp.parabox.ui.contact

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ojhdtapp.parabox.core.util.ImageUtil
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.BlurTransformation
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.common.SystemUiController
import com.ojhdtapp.parabox.ui.common.placeholder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ContactDetailPage(
    modifier: Modifier = Modifier,
    viewModel: ContactPageViewModel,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Nothing>,
    mainSharedState: MainSharedState,
    layoutType: ContactLayoutType,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val systemUiController = LocalSystemUiController.current
    if (layoutType == ContactLayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Crossfade(targetState = state.contactDetail.contactWithExtensionInfo == null, label = "") {
                if (it) {
                    Column(
                        modifier = modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "选择联系人",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    ContactDetailPageContent(
                        state = state,
                        scaffoldNavigator = scaffoldNavigator,
                        systemUiController = systemUiController,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        onMainSharedEvent = onMainSharedEvent
                    )
                }
            }
        }
    } else {
        ContactDetailPageContent(
            state = state,
            scaffoldNavigator = scaffoldNavigator,
            systemUiController = systemUiController,
            mainSharedState = mainSharedState,
            layoutType = layoutType,
            onMainSharedEvent = onMainSharedEvent
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun ContactDetailPageContent(
    modifier: Modifier = Modifier,
    state: ContactPageState,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Nothing>,
    systemUiController: SystemUiController,
    mainSharedState: MainSharedState,
    layoutType: ContactLayoutType,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val scrollState = rememberScrollState()
    val backgroundHeightPx = with(LocalDensity.current) { 108.dp.toPx() }
    val isScrolled by remember {
        derivedStateOf { scrollState.value > backgroundHeightPx }
    }
    var isBackgroundLight by remember { mutableStateOf(true) }
    var navigationIconTint =
        if (isBackgroundLight xor isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.inverseOnSurface
    val navigationContentColor by animateColorAsState(targetValue = if (isScrolled) MaterialTheme.colorScheme.onSurface else navigationIconTint)
    LaunchedEffect(isScrolled) {
        if (layoutType == ContactLayoutType.NORMAL) {
            if (isScrolled) {
                systemUiController.reset()
            } else {
                systemUiController.setStatusBarColor(isBackgroundLight)

            }
        }
    }
    BackHandler(layoutType == ContactLayoutType.NORMAL) {
        scaffoldNavigator.navigateBack()
        systemUiController.reset()
        onMainSharedEvent(MainSharedEvent.ShowNavigationBar(true))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (layoutType == ContactLayoutType.NORMAL) {
                        IconButton(
                            onClick = {
                                scaffoldNavigator.navigateBack()
                                systemUiController.reset()
                                onMainSharedEvent(MainSharedEvent.ShowNavigationBar(true))
                            }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "back",
                                tint = navigationContentColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val offsetDp by remember {
                derivedStateOf {
                    with(density) {
                        -(scrollState.value / 3).toDp()
                    }
                }
            }
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .offset(y = offsetDp),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.contactDetail.contactWithExtensionInfo?.contact?.avatar?.getModel())
                    .transformations(BlurTransformation(LocalContext.current))
                    .build(),
                contentDescription = "avatar_bg", contentScale = ContentScale.Crop,
                onSuccess = {
                    if (layoutType == ContactLayoutType.NORMAL) {
                        (it.result.drawable as? BitmapDrawable)?.bitmap?.also { bitmap ->
                            isBackgroundLight = ImageUtil.checkBitmapLight(bitmap)
                            systemUiController.setStatusBarColor(isBackgroundLight)
                        }
                    }
                }
            )
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp), contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        ),
                        color = if (layoutType == ContactLayoutType.NORMAL) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
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
                                model = state.contactDetail.contactWithExtensionInfo?.contact?.avatar?.getModel(),
                                name = state.contactDetail.contactWithExtensionInfo?.contact?.name ?: "null"
                            )
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (layoutType == ContactLayoutType.NORMAL) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = state.contactDetail.contactWithExtensionInfo?.contact?.name ?: "null",
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
                                    text = state.contactDetail.contactWithExtensionInfo?.extensionInfo?.name ?: "",
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = state.contactDetail.contactWithExtensionInfo?.extensionInfo?.pkg ?: "",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    text = state.contactDetail.contactWithExtensionInfo?.extensionInfo?.alias ?: "",
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

                        when (state.contactDetail.relativeChatState.loadState) {
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
                                    state.contactDetail.relativeChatState.chatList.forEach {
                                        RelativeChatItem(chat = it) {

                                        }
                                    }
                                }
                            }
                        }

                    }
                    Spacer(modifier = Modifier.height(560.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyRelativeChatItem(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            modifier = modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.placeholder(
                    isLoading = true
                ),
                text = "chat_name",
                maxLines = 1
            )
        }
    }
}

@Composable
fun RelativeChatItem(modifier: Modifier = Modifier, chat: Chat, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick
    ) {
        Row(
            modifier = modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.secondary),
            ) {
                CommonAvatar(
                    model = CommonAvatarModel(
                        model = chat.avatar.getModel(),
                        name = chat.name
                    )
                )

            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = chat.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}