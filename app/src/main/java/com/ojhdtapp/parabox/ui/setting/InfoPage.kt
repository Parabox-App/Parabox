package com.ojhdtapp.parabox.ui.setting

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.message_content.Image
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.message.MessageState
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.SettingNavGraph
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.rememberPreviewerState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun InfoPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit,
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val colorTransitionFraction = scrollBehavior.state.collapsedFraction
            val appBarContainerColor by rememberUpdatedState(
                lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    FastOutLinearInEasing.transform(colorTransitionFraction)
                )
            )
            LargeTopAppBar(
                modifier = Modifier
                    .background(appBarContainerColor)
                    .statusBarsPadding(),
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        IconButton(onClick = {
                            mainNavController.navigateUp()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "back"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) {
        // Plugin List State
        val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()
        LazyColumn(
            contentPadding = it
        ) {
            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Medium) {
                item {
                    ThemeBlock(
                        modifier = Modifier.fillMaxWidth(),
                        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
                        version = BuildConfig.VERSION_NAME,
                        onBlockClick = {},
                        onUserNameClick = {
                            viewModel.setEditUserNameDialogState(true)
                        },
                        onVersionClick = {}
                    )
                }
            }
        }
//        val context = LocalContext.current
//        val imageList =
//            lazyPagingItems.itemSnapshotList.items.fold(mutableListOf<Pair<Long, Painter>>()) { acc, message ->
//                val imageMessageList = message.contents.filterIsInstance<Image>()
//                val lastIndex = imageMessageList.lastIndex
//                imageMessageList.reversed().forEachIndexed { index, t ->
//                    if (t.uriString != null || t.url != null) {
//                        val painter = rememberAsyncImagePainter(
//                            model = ImageRequest.Builder(context)
//                                .data(t.uriString ?: t.url)
//                                .build()
//                        )
//                        acc.add(
//                            "${message.messageId}${
//                                (lastIndex - index).coerceIn(
//                                    0,
//                                    lastIndex
//                                )
//                            }".toLong()
//                                    to painter
//                        )
//                    }
//                }
//                acc
//            }
//        val imageViewerState = rememberPreviewerState(enableVerticalDrag = true){
//            imageList[it].first
//        }
//        LaunchedEffect(messageState) {
//            imageViewerState.close()
//        }
//        ImagePreviewer(
//            modifier = Modifier.fillMaxSize(),
//            count = imageList.size,
//            state = imageViewerState,
//            // 图片加载器
//            imageLoader = { index ->
//                imageList[index].second
//            },
//            detectGesture = {
//                // 点击手势
//                onTap = {
//                    scope.launch {
//                        // 关闭预览，带转换效果
//                        previewerState.closeTransform()
//                    }
//                }
//            }
//        )
    }
}