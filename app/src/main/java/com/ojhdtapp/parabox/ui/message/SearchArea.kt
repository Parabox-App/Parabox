package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.core.util.splitKeeping
import com.ojhdtapp.parabox.domain.model.message_content.getContentString
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.ChatPageDestination
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ramcosta.composedestinations.navigation.navigate
import com.valentinilk.shimmer.Shimmer
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.SearchArea(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    viewModel: MessagePageViewModel,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    shimmerInstance: Shimmer,
    mainNavController: NavController
) {
    val messageSearchResult = viewModel.messageSearchResultStateFlow.collectAsState()
    val contactSearchResult = viewModel.contactSearchResultStateFlow.collectAsState()
    val searchText by viewModel.searchText
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = searchText.isBlank()
        ) {
            if (it) {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = paddingValues
                ) {
                    item {
                        if (viewModel.personalContactState.value.data.isEmpty() && viewModel.groupContactState.value.data.isEmpty()) {
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
                        if (viewModel.personalContactState.value.data.isNotEmpty()) {
                            Text(
                                modifier = modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                text = "最近联系人",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(
                                items = viewModel.personalContactState.value.data,
                                key = { it.contactId }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            mainSharedViewModel.loadMessageFromContact(it)
                                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                                                mainNavController.navigate(ChatPageDestination())
                                            }
                                        }
                                        .padding(16.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(it.profile.avatar)
                                            .crossfade(true)
                                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                            .build(),
                                        contentDescription = "avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        modifier = Modifier.widthIn(0.dp, 48.dp),
                                        text = it.profile.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    item {
                        if (viewModel.groupContactState.value.data.isNotEmpty()) {
                            Text(
                                modifier = modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                text = "最近会话",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(items = viewModel.groupContactState.value.data, key = { it.contactId }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mainSharedViewModel.loadMessageFromContact(it)
                                    if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                                        mainNavController.navigate(ChatPageDestination())
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(it.profile.avatar)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                    .build(),
                                contentDescription = "avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = it.profile.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = paddingValues,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        if (messageSearchResult.value.data.isEmpty() && contactSearchResult.value.data.isEmpty()
                            && !messageSearchResult.value.isLoading && !contactSearchResult.value.isLoading
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
                        if (messageSearchResult.value.data.isNotEmpty()) {
                            Text(
                                modifier = modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                text = "消息",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(
                        items = messageSearchResult.value.data,
                        key = { it.messages.first().messageId }) { cm ->
                        var expanded by remember {
                            mutableStateOf(false)
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expanded = !expanded
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(cm.contact.profile.avatar)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                        .build(),
                                    contentDescription = "avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column() {
                                    Text(
                                        text = cm.contact.profile.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "包含${cm.messages.size}条相关聊天记录",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            AnimatedVisibility(visible = expanded) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    cm.messages.forEach {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    mainSharedViewModel.navigateToChatPage(
                                                        cm.contact,
                                                        it
                                                    )
                                                    if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                                                        mainNavController.navigate(
                                                            ChatPageDestination()
                                                        )
                                                    }
                                                }
                                                .padding(horizontal = 28.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(it.profile.avatar)
                                                    .crossfade(true)
                                                    .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                    .build(),
                                                contentDescription = "avatar",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(28.dp))
                                            Text(
                                                text = buildAnnotatedString {
                                                    withStyle(
                                                        style = SpanStyle(
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    ) {
                                                        append(it.profile.name)
                                                        append(": ")
                                                    }
                                                    it.contents.getContentString()
                                                        .splitKeeping(searchText).forEach {
                                                            if (it == searchText) {
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        if (contactSearchResult.value.data.isNotEmpty()) {
                            Text(
                                modifier = modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                text = "会话",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(items = contactSearchResult.value.data, key = { it.contactId }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mainSharedViewModel.loadMessageFromContact(it)
                                    if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                                        mainNavController.navigate(ChatPageDestination())
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(it.profile.avatar)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                    .build(),
                                contentDescription = "avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = buildAnnotatedString {
                                    it.profile.name.splitKeeping(searchText).forEach {
                                        if (it == searchText) {
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
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}