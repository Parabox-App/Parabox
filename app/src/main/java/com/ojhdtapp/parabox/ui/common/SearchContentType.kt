package com.ojhdtapp.parabox.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.splitKeeping
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import kotlinx.coroutines.launch

@Composable
fun SearchContent(modifier: Modifier = Modifier, state: MainSharedState, onEvent: (e: MainSharedEvent) -> Unit) {
    val searchPageState by remember(state.search) {
        derivedStateOf {
            when {
                state.search.showRecent && state.search.query.isBlank() -> SearchContentType.RECENT
                state.search.showRecent && state.search.query.isNotBlank() -> SearchContentType.TYPING
                else -> SearchContentType.DONE
            }
        }
    }
    Crossfade(targetState = searchPageState, modifier = modifier, label = "search_pages") {
        when (it) {
            SearchContentType.RECENT -> {
                RecentSearchContent(state = state.search, onEvent = onEvent)
            }

            SearchContentType.TYPING -> {
                TypingSearchContent(state = state.search, onEvent = onEvent)
            }

            SearchContentType.DONE -> {
                DoneSearchContent(state = state.search)
            }
        }
    }
}

enum class SearchContentType {
    RECENT, TYPING, DONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentSearchContent(
    modifier: Modifier = Modifier,
    state: MainSharedState.Search,
    onEvent: (e: MainSharedEvent) -> Unit
) {
    LazyColumn() {
        item {
            if (state.recentQuery.isEmpty() && state.recentQueryState != LoadState.LOADING
                && state.chat.result.isEmpty() && state.chat.loadState != LoadState.LOADING
                && state.contact.result.isEmpty() && state.contact.loadState != LoadState.LOADING
                && state.message.result.isEmpty() && state.message.loadState != LoadState.LOADING) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "search result",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "搜索 Parabox",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = state.recentQueryState != LoadState.LOADING && state.recentQuery.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "近期搜索",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                state.recentQuery.forEach {
                    SearchResultItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(
                                visible = state.chat.loadState == LoadState.LOADING,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                highlight = PlaceholderHighlight.fade(),
                            ),
                        avatarModel = null,
                        title = buildAnnotatedString { append(it.value) },
                        subTitle = null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "history",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(modifier = Modifier.clickable {
                                onEvent(MainSharedEvent.DeleteRecentQuery(it.id))
                            }, imageVector = Icons.Outlined.Close, contentDescription = "delete recent query")
                        }
                    ) {
                        onEvent(MainSharedEvent.SearchConfirm(it.value))
                    }
                }
            }
        }
//        item {
//            LazyRow(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
//            ) {
//                items(items = state.recentQuery) {
//                    InputChip(
//                        modifier = Modifier.placeholder(
//                            visible = state.recentQueryState == LoadState.LOADING,
//                            color = MaterialTheme.colorScheme.secondaryContainer,
//                            shape = MaterialTheme.shapes.small,
//                            highlight = PlaceholderHighlight.fade(),
//                        ),
//                        selected = false,
//                        onClick = { onEvent(MainSharedEvent.QueryInput(it.value)) },
//                        label = { Text(text = it.value) },
//                        trailingIcon = {
//                            Icon(
//                                modifier = Modifier.clickable {
//                                    onEvent(MainSharedEvent.DeleteRecentQuery(it.id))
//                                },
//                                imageVector = Icons.Outlined.Close,
//                                tint = MaterialTheme.colorScheme.onSurface,
//                                contentDescription = "delete recent query"
//                            )
//                        })
//                }
//            }
//        }
        item {
            if (state.chat.result.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "会话",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                state.chat.result.forEach {
                    SearchResultItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(
                                visible = state.chat.loadState == LoadState.LOADING,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                highlight = PlaceholderHighlight.fade(),
                            ),
                        avatarModel = it.avatar.getModel(),
                        title = buildAnnotatedString {
                            it.name.splitKeeping(state.query).forEach {
                                if (it == state.query) {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        append(it)
                                    }
                                } else {
                                    append(it)
                                }
                            }
                        },
                        subTitle = null
                    ) {

                    }
                }
            }
        }
        item {
            if (state.contact.result.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "联系人",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                state.contact.result.forEach {
                    SearchResultItemVertical(
                        modifier = Modifier.placeholder(
                            visible = state.contact.loadState == LoadState.LOADING,
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            highlight = PlaceholderHighlight.fade(),
                        ),
                        avatarModel = it.avatar.getModel(),
                        title = buildAnnotatedString {
                            it.name.splitKeeping(state.query).forEach {
                                if (it == state.query) {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        append(it)
                                    }
                                } else {
                                    append(it)
                                }
                            }
                        },
                        subTitle = null
                    ) {

                    }
                }
            }
        }
        item {
            if (state.message.result.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "消息",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .placeholder(
                        visible = state.message.loadState == LoadState.LOADING,
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                state.message.result.forEach {
                    SearchResultItem(
                        modifier = Modifier.fillMaxWidth(),
                        avatarModel = it.chat!!.avatar.getModel(),
                        title = buildAnnotatedString {
                            it.chat.name.splitKeeping(state.query).forEach {
                                if (it == state.query) {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        append(it)
                                    }
                                } else {
                                    append(it)
                                }
                            }
                        },
                        subTitle = buildAnnotatedString {
                            append(it.contact!!.name)
                            append(": ")
                            it.message.contentString.splitKeeping(state.query).forEach {
                                if (it == state.query) {
                                    withStyle(
                                        style = SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        append(it)
                                    }
                                } else {
                                    append(it)
                                }
                            }
                        },
                    ) {

                    }
                }
            }
        }
    }
}

@Composable
fun TypingSearchContent(
    modifier: Modifier = Modifier,
    state: MainSharedState.Search,
    onEvent: (e: MainSharedEvent) -> Unit
) {
    LazyColumn() {
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                state.recentQuery.forEach {
                    SearchResultItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(
                                visible = state.chat.loadState == LoadState.LOADING,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                highlight = PlaceholderHighlight.fade(),
                            ),
                        avatarModel = null,
                        title = buildAnnotatedString { append(it.value) },
                        subTitle = null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "history",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(modifier = Modifier.clickable {
                                onEvent(MainSharedEvent.DeleteRecentQuery(it.id))
                            }, imageVector = Icons.Outlined.Close, contentDescription = "delete recent query")
                        }
                    ) {
                        onEvent(MainSharedEvent.SearchConfirm(it.value))
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DoneSearchContent(modifier: Modifier = Modifier, state: MainSharedState.Search) {
    val coroutineScope = rememberCoroutineScope()
    Column() {
        val tabList = listOf<String>(
            stringResource(id = R.string.confirm),
            stringResource(id = R.string.confirm),
            stringResource(id = R.string.confirm)
        )
        val pagerState = rememberPagerState() { tabList.size }
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabList.forEachIndexed { index, s ->
                Tab(selected = index == pagerState.currentPage, onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }, text = { Text(text = s) })
            }
        }
        HorizontalPager(state = pagerState) {

        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchResultItem(
    modifier: Modifier = Modifier,
    avatarModel: Any?,
    title: AnnotatedString,
    subTitle: AnnotatedString?,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        elevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(32.dp)
                    .background(
                        if (leadingIcon != null) MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        else MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                leadingIcon?.invoke() ?: SubcomposeAsyncImage(
                    model = avatarModel,
                    contentDescription = "chat_avatar",
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val state = painter.state
                    val namedAvatarBm =
                        AvatarUtil.createNamedAvatarBm(
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.toArgb(),
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer.toArgb(),
                            name = title.text
                        ).asImageBitmap()
                    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .placeholder(
                                    visible = state is AsyncImagePainter.State.Loading,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    highlight = PlaceholderHighlight.fade(),
                                ),
                            bitmap = namedAvatarBm,
                            contentDescription = "named_avatar"
                        )
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f), verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = if (subTitle?.isNotBlank() == true) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (subTitle?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailingIcon()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchResultItemVertical(
    modifier: Modifier = Modifier,
    avatarModel: Any?,
    title: AnnotatedString,
    subTitle: AnnotatedString?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        elevation = 0.dp,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .width(width = 96.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = avatarModel,
                    contentDescription = "chat_avatar",
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val state = painter.state
                    val namedAvatarBm =
                        AvatarUtil.createNamedAvatarBm(
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.toArgb(),
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer.toArgb(),
                            name = title.text
                        ).asImageBitmap()
                    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .placeholder(
                                    visible = state is AsyncImagePainter.State.Loading,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    highlight = PlaceholderHighlight.fade(),
                                ),
                            bitmap = namedAvatarBm,
                            contentDescription = "named_avatar"
                        )
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subTitle?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}