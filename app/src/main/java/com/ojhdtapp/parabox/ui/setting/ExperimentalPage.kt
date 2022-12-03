package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun ExperimentalPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
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
                title = { Text(stringResource(R.string.experimental)) },
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
        LazyColumn(
            contentPadding = it
        ) {
            item {
                Column(modifier = Modifier.padding(24.dp, 16.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.experimental_info),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                PreferencesCategory(text = stringResource(R.string.ml_kit))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.entity_extraction_title),
                    subtitleOn = stringResource(R.string.entity_extraction_subtitle_on),
                    subtitleOff = stringResource(R.string.entity_extraction_subtitle_off),
                    checked = viewModel.entityExtractionFlow.collectAsState(initial = true).value,
                    enabled = true,
                    onCheckedChange = viewModel::setEntityExtraction
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.smart_reply_title),
                    subtitleOn = stringResource(R.string.smart_reply_subtitle_on),
                    subtitleOff = stringResource(R.string.smart_reply_subtitle_off),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {}
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.translation_title),
                    subtitleOn = stringResource(R.string.translation_subtitle_on),
                    subtitleOff = stringResource(R.string.translation_subtitle_off),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {}
                )
            }
            item {
                PreferencesCategory(text = stringResource(R.string.notification_and_bubble))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.return_home_in_bubble_title),
                    subtitleOn = stringResource(R.string.return_home_in_bubble_subtitle_on),
                    subtitleOff = stringResource(R.string.return_home_in_bubble_subtitle_off),
                    checked = viewModel.allowBubbleHomeFlow.collectAsState(initial = false).value,
                    onCheckedChange = viewModel::setAllowBubbleHome
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.notification_when_foreground_title),
                    subtitleOn = stringResource(R.string.notification_when_foreground_subtitle_on),
                    subtitleOff = stringResource(R.string.notification_when_foreground_subtitle_off),
                    checked = viewModel.allowForegroundNotificationFlow.collectAsState(initial = false).value,
                    onCheckedChange = viewModel::setAllowForegroundNotification
                )
            }
        }
    }
}