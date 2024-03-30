package com.ojhdtapp.parabox.ui.common

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.splitKeeping
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.model.filter.MessageFilter
import com.ojhdtapp.parabox.domain.use_case.Query.Companion.SEARCH_RECENT_DATA_NUM
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import kotlinx.coroutines.launch
import me.saket.cascade.CascadeDropdownMenu

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
                DoneSearchContent(state = state.search, onEvent = onEvent)
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
                && state.message.result.isEmpty() && state.message.loadState != LoadState.LOADING
            ) {
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
                visible = state.recentQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
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
                if (state.chat.loadState == LoadState.LOADING && state.query.isNotBlank()) {
                    repeat(3) {
                        EmptySearchResultItem()
                    }
                } else {
                    state.recentQuery.forEach {
                        SearchResultItem(
                            modifier = Modifier
                                .fillMaxWidth(),
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
            AnimatedVisibility(
                visible = state.chat.result.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "近期会话",
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
                if (state.chat.loadState == LoadState.LOADING && state.query.isNotBlank()) {
                    repeat(3) {
                        EmptySearchResultItem()
                    }
                } else {
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
        }
        item {
            AnimatedVisibility(
                visible = state.contact.result.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (state.contact.result.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = "近期联系人",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                state.contact.result.forEach {
                    SearchResultItemVertical(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .placeholder(
                                visible = state.contact.loadState == LoadState.LOADING,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                shape = MaterialTheme.shapes.large,
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
            AnimatedVisibility(
                visible = state.message.result.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "最新消息",
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
                if (state.message.loadState == LoadState.LOADING && state.query.isNotBlank()) {
                    repeat(3) {
                        EmptySearchResultItem()
                    }
                } else {
                    state.message.result.forEach {
                        SearchResultItem(
                            modifier = Modifier.fillMaxWidth(),
                            avatarModel = it.contact?.avatar?.getModel(),
                            subTitle = buildAnnotatedString {
                                it.chat!!.name.splitKeeping(state.query).forEach {
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
                            title = buildAnnotatedString {
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
        item {
            Spacer(modifier = Modifier.height(16.dp))
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
                            .fillMaxWidth(),
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
        item {
            if (state.chat.result.isEmpty() && state.chat.loadState != LoadState.LOADING
                && state.contact.result.isEmpty() && state.contact.loadState != LoadState.LOADING
                && state.message.result.isEmpty() && state.message.loadState != LoadState.LOADING
            ) {
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
                        text = "无搜索结果",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = state.chat.loadState == LoadState.LOADING || state.chat.result.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
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
                if (state.chat.loadState == LoadState.LOADING && state.query.isNotBlank()) {
                    repeat(3) {
                        EmptySearchResultItem()
                    }
                } else {
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
                    if (state.chat.result.size >= SEARCH_RECENT_DATA_NUM) {
                        SearchResultItem(
                            modifier = Modifier
                                .fillMaxWidth(),
                            avatarModel = null,
                            title = buildAnnotatedString { append("查看完整结果") },
                            subTitle = null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "more",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                Icon(imageVector = Icons.Outlined.ArrowOutward, contentDescription = "jump")
                            }
                        ) {
                            onEvent(MainSharedEvent.SearchConfirm(state.query))
                        }
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
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .placeholder(
                                visible = state.contact.loadState == LoadState.LOADING,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                shape = MaterialTheme.shapes.large,
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
            AnimatedVisibility(
                visible = state.message.loadState == LoadState.LOADING || state.message.result.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    .clip(MaterialTheme.shapes.extraLarge),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (state.message.loadState == LoadState.LOADING && state.query.isNotBlank()) {
                    repeat(3) {
                        EmptySearchResultItem()
                    }
                } else {
                    state.message.result.forEach {
                        SearchResultItem(
                            modifier = Modifier.fillMaxWidth(),
                            avatarModel = it.chat!!.avatar.getModel(),
                            subTitle = buildAnnotatedString {
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
                            title = buildAnnotatedString {
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
                if (state.message.result.size >= SEARCH_RECENT_DATA_NUM) {
                    SearchResultItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        avatarModel = null,
                        title = buildAnnotatedString { append("查看完整结果") },
                        subTitle = null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "more",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(imageVector = Icons.Outlined.ArrowOutward, contentDescription = "jump")
                        }
                    ) {
                        onEvent(MainSharedEvent.SearchConfirm(state.query))
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DoneSearchContent(
    modifier: Modifier = Modifier,
    state: MainSharedState.Search,
    onEvent: (e: MainSharedEvent) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Column() {
        val tabList = listOf<String>(
            "消息",
            "联系人",
            "会话"
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
                    DoneSearchMessageContent(modifier = Modifier.fillMaxSize(), state = state, onEvent = onEvent)
                }

                1 -> {
                    DoneSearchContactContent(modifier = Modifier.fillMaxSize(), state = state, onEvent = onEvent)
                }

                2 -> {
                    DoneSearchChatContent(modifier = Modifier.fillMaxSize(), state = state, onEvent = onEvent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoneSearchMessageContent(
    modifier: Modifier = Modifier,
    state: MainSharedState.Search,
    onEvent: (e: MainSharedEvent) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                state.message.filterList.forEach {
                    var isMenuVisible by rememberSaveable { mutableStateOf(false) }
                    Box(modifier = Modifier.wrapContentSize()) {
                        MyFilterChip(
                            selected = it !is MessageFilter.SenderFilter.All && it !is MessageFilter.ChatFilter.All && it !is MessageFilter.DateFilter.All,
                            label = { Text(text = it.getLabel(context) ?: stringResource(id = it.labelResId)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "expand",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }) {
                            when (it) {
                                is MessageFilter.SenderFilter -> {
                                    if (it is MessageFilter.SenderFilter.All) {
                                        onEvent(MainSharedEvent.PickContact {
                                            if (it != null) {
                                                onEvent(
                                                    MainSharedEvent.UpdateSearchDoneMessageFilter(
                                                        MessageFilter.SenderFilter.Custom(
                                                            senderName = it.name,
                                                            senderId = it.contactId
                                                        )
                                                    )
                                                )
                                            } else {

                                            }
                                        })
                                    } else {
                                        onEvent(
                                            MainSharedEvent.UpdateSearchDoneMessageFilter(
                                                MessageFilter.SenderFilter.All
                                            )
                                        )
                                    }
                                }

                                is MessageFilter.ChatFilter -> {
                                    if (it is MessageFilter.ChatFilter.All) {
                                        onEvent(MainSharedEvent.PickChat {
                                            Log.d("parabox", "pick chat success:${it}")
                                            if (it != null) {
                                                onEvent(
                                                    MainSharedEvent.UpdateSearchDoneMessageFilter(
                                                        MessageFilter.ChatFilter.Custom(
                                                            chatName = it.name,
                                                            chatId = it.chatId
                                                        )
                                                    )
                                                )
                                            } else {

                                            }
                                        })
                                    } else {
                                        onEvent(
                                            MainSharedEvent.UpdateSearchDoneMessageFilter(
                                                MessageFilter.ChatFilter.All
                                            )
                                        )
                                    }
                                }

                                is MessageFilter.DateFilter -> {
                                    isMenuVisible = true
                                }

                                else -> {}
                            }
                        }
                        if (it is MessageFilter.DateFilter) {
                            CascadeDropdownMenu(
                                expanded = isMenuVisible, onDismissRequest = { isMenuVisible = false },
                                properties = PopupProperties(
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true,
                                    focusable = true
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Log.d("parabox", "list:${MessageFilter.DateFilter.allFilterList}")
                                androidx.compose.material3.DropdownMenuItem(text = {
                                    Text(text = stringResource(id = R.string.time_filter_all_label))
                                }, onClick = {
                                    isMenuVisible = false
                                    onEvent(MainSharedEvent.UpdateSearchDoneMessageFilter(MessageFilter.DateFilter.All))
                                })
                                MessageFilter.DateFilter.allFilterList.forEach {
                                    androidx.compose.material3.DropdownMenuItem(text = {
                                        Text(text = it.label ?: stringResource(id = it.labelResId))
                                    }, onClick = {
                                        isMenuVisible = false
                                        onEvent(MainSharedEvent.UpdateSearchDoneMessageFilter(it))
                                    })
                                }
                                androidx.compose.material3.DropdownMenuItem(text = {
                                    Text(text = "自定义")
                                }, onClick = {
                                    isMenuVisible = false
                                    onEvent(MainSharedEvent.PickDateRange {
                                        if (it != null) {
                                            onEvent(
                                                MainSharedEvent.UpdateSearchDoneMessageFilter(
                                                    MessageFilter.DateFilter.Custom(
                                                        timestampStart = it.first,
                                                        timestampEnd = it.second
                                                    )
                                                )
                                            )
                                        } else {

                                        }
                                    })
                                })
                            }
                        }
                    }

                }
            }
        }
        if (state.message.loadState == LoadState.LOADING) {
            itemsIndexed(items = listOf(1, 2, 3)) { index, res ->
                val topRadius = if (index == 0) 24.dp else 0.dp
                val bottomRadius = if (index == 2) 24.dp else 0.dp
                EmptySearchResultItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = topRadius,
                                topEnd = topRadius,
                                bottomStart = bottomRadius,
                                bottomEnd = bottomRadius
                            )
                        )
                )
            }
        }
        itemsIndexed(items = state.message.filterResult) { index, res ->
            val topRadius = if (index == 0) 24.dp else 0.dp
            val bottomRadius = if (index == state.message.filterResult.lastIndex) 24.dp else 0.dp
            SearchResultItem(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = topRadius,
                            topEnd = topRadius,
                            bottomStart = bottomRadius,
                            bottomEnd = bottomRadius
                        )
                    ),
                avatarModel = res.contact?.avatar?.getModel(),
                subTitle = buildAnnotatedString {
                    res.chat!!.name.splitKeeping(state.query).forEach {
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
                title = buildAnnotatedString {
                    append(res.contact!!.name)
                    append(": ")
                    res.message.contentString.splitKeeping(state.query).forEach {
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
        item {
            if (state.message.filterResult.isEmpty() && state.message.loadState != LoadState.LOADING
            ) {
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
                        text = "无搜索结果",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun DoneSearchContactContent(
    modifier: Modifier = Modifier,
    state: MainSharedState.Search,
    onEvent: (e: MainSharedEvent) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (state.contact.loadState == LoadState.LOADING) {
            itemsIndexed(items = listOf(1, 2, 3)) { index, res ->
                val topRadius = if (index == 0) 24.dp else 0.dp
                val bottomRadius = if (index == 2) 24.dp else 0.dp
                EmptySearchResultItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = topRadius,
                                topEnd = topRadius,
                                bottomStart = bottomRadius,
                                bottomEnd = bottomRadius
                            )
                        )
                )
            }
        }
        itemsIndexed(items = state.contact.result) { index, res ->
            val topRadius = if (index == 0) 24.dp else 0.dp
            val bottomRadius = if (index == state.contact.result.lastIndex) 24.dp else 0.dp
            SearchResultItem(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = topRadius,
                            topEnd = topRadius,
                            bottomStart = bottomRadius,
                            bottomEnd = bottomRadius
                        )
                    ),
                avatarModel = res.avatar.getModel(),
                title = buildAnnotatedString {
                    res.name.splitKeeping(state.query).forEach {
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
                subTitle = null,
            ) {

            }
        }
        item {
            if (state.contact.result.isEmpty() && state.contact.loadState != LoadState.LOADING) {
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
                        text = "无搜索结果",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun DoneSearchChatContent(
    modifier: Modifier = Modifier,
    state: MainSharedState.Search,
    onEvent: (e: MainSharedEvent) -> Unit
) {
    LazyColumn(modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
        item {
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                ChatFilter.allFilterList.forEach {
                    MyFilterChip(
                        selected = it in state.chat.enabledFilterList,
                        label = { Text(text = it.label ?: stringResource(id = it.labelResId)) }) {
                        onEvent(MainSharedEvent.UpdateSearchDoneChatFilter(it))
                    }
                }
            }
        }
        if (state.message.loadState == LoadState.LOADING) {
            itemsIndexed(items = listOf(1, 2, 3)) { index, res ->
                val topRadius = if (index == 0) 24.dp else 0.dp
                val bottomRadius = if (index == 2) 24.dp else 0.dp
                EmptySearchResultItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = topRadius,
                                topEnd = topRadius,
                                bottomStart = bottomRadius,
                                bottomEnd = bottomRadius
                            )
                        )
                )
            }
        }
        itemsIndexed(items = state.chat.filterResult) { index, res ->
            val topRadius = if (index == 0) 24.dp else 0.dp
            val bottomRadius = if (index == state.chat.filterResult.lastIndex) 24.dp else 0.dp
            SearchResultItem(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = topRadius,
                            topEnd = topRadius,
                            bottomStart = bottomRadius,
                            bottomEnd = bottomRadius
                        )
                    ),
                avatarModel = res.avatar.getModel(),
                title = buildAnnotatedString {
                    res.name.splitKeeping(state.query).forEach {
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
                subTitle = null,
            ) {

            }
        }
        item {
            if (state.chat.filterResult.isEmpty() && state.chat.loadState != LoadState.LOADING) {
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
                        text = "无搜索结果",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
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
                leadingIcon?.invoke() ?: CommonAvatar(model = CommonAvatarModel(model = avatarModel, name = title.text))
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
fun EmptySearchResultItem(
    modifier: Modifier = Modifier,
    hasSubtitle: Boolean = false,
) {
    Surface(
        modifier = modifier,
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
                    .background(MaterialTheme.colorScheme.primary)
                    .placeholder(
                        visible = true,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                contentAlignment = Alignment.Center
            ) {}
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f), verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.placeholder(
                        visible = true,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                    text = "title",
                    style = if (hasSubtitle) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (hasSubtitle) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        modifier = Modifier.placeholder(
                            visible = true,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            highlight = PlaceholderHighlight.fade(),
                        ),
                        text = "subtitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
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
                .width(width = 80.dp)
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
                CommonAvatar(model = CommonAvatarModel(model = avatarModel, name = title.text) )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
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