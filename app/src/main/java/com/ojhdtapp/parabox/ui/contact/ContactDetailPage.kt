package com.ojhdtapp.parabox.ui.contact

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Message
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
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil3.request.transformations
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.ImageUtil
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.BlurTransformation
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController

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
    Crossfade(targetState = state.contactDetail.contactWithExtensionInfo != null, label = "") {
        if (it) {
            var isBackgroundLight by remember { mutableStateOf(true) }
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
                            if(layoutType == ContactLayoutType.NORMAL) {
                                IconButton(onClick = {
                                    scaffoldNavigator.navigateBack()
                                    systemUiController.reset()
                                    onMainSharedEvent(MainSharedEvent.ShowNavigationBar(true))
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowBack, contentDescription = "back",
                                        tint = if (isBackgroundLight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
            { innerPadding ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp), contentAlignment = Alignment.BottomCenter
                    ) {
                        AsyncImage(
                            modifier = Modifier.fillMaxSize(),
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(state.contactDetail.contactWithExtensionInfo?.contact?.avatar?.getModel())
                                .transformations(BlurTransformation(LocalContext.current))
                                .build(),
                            contentDescription = "avatar_bg", contentScale = ContentScale.Crop,
                            onSuccess = {
                                (it.result.drawable as? BitmapDrawable)?.bitmap?.also { bitmap ->
                                    isBackgroundLight = ImageUtil.checkBitmapLight(bitmap)
                                    systemUiController.setStatusBarColor(isBackgroundLight)
                                }
                            }
                        )
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                topEnd = 28.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
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
                                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CommonAvatar(
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
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface),
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
                        Card(
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(text = "信息")
                            Row {
                                Text(text = state.contactDetail.contactWithExtensionInfo?.extensionInfo?.name ?: "")
                            }
                        }
                        Card(
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(text = "加入的群组")
                            when (state.contactDetail.relativeChatState.loadState) {
                                LoadState.LOADING -> {

                                }

                                LoadState.ERROR -> {
                                    Text(text = "加载出错，请稍后再试")
                                }

                                LoadState.SUCCESS -> {
                                    Column {
                                        state.contactDetail.relativeChatState.chatList.forEach {
                                            Text(text = it.name)
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    modifier = modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.select_conversation),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}