package com.ojhdtapp.parabox.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.base.UiEvent
import kotlinx.coroutines.launch

@Composable
fun SearchContent(modifier: Modifier = Modifier, state: MainSharedState.Search, onEvent: (e: MainSharedEvent) -> Unit) {
    val searchPageState by remember {
        derivedStateOf {
            when {
                state.showRecent && state.query.isEmpty() -> SearchContent.RECENT
                state.showRecent && state.query.isNotBlank() -> SearchContent.TYPING
                else -> SearchContent.DONE
            }
        }
    }
    Crossfade(targetState = searchPageState, modifier = modifier) {
        when (it) {
            SearchContent.RECENT -> {
                RecentSearchContent()
            }

            SearchContent.TYPING -> {
                TypingSearchContent()
            }

            SearchContent.DONE -> {
                DoneSearchContent(state = state)
            }
        }
    }
}

enum class SearchContent {
    RECENT, TYPING, DONE
}

@Composable
fun RecentSearchContent(modifier: Modifier = Modifier) {
}

@Composable
fun TypingSearchContent(modifier: Modifier = Modifier) {
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

