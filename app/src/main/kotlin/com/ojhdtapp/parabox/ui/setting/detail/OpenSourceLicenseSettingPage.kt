package com.ojhdtapp.parabox.ui.setting.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.ojhdtapp.parabox.core.util.BrowserUtil
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.navigation.DefaultSettingComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun OpenSourceLicenseSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    BackHandler {
        navigation.pop()
    }
    if (layoutType == LayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()

                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        navigation.pop()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        color = MaterialTheme.colorScheme.onSurface,
                        text = state.labelDetailState.selected?.label ?: "开放源代码许可",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Content(
                    modifier = Modifier.weight(1f),
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    navigation = navigation,
                    stackState = stackState,
                    onEvent = onEvent,
                    onMainSharedEvent = onMainSharedEvent,
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = state.labelDetailState.selected?.label ?: "开放源代码许可",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigation.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
                )
            }
        ) { innerPadding ->
            Content(
                modifier = Modifier.padding(innerPadding),
                state = state,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                navigation = navigation,
                stackState = stackState,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent
            )
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) =
    LazyColumn(modifier = modifier) {
        items(items = License.defaultList) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            SettingItem(title = it.name, subTitle = it.license, selected = false, layoutType = layoutType) {
                coroutineScope.launch {
                    BrowserUtil.launchURL(context, it.url)
                }
            }
        }
    }

data class License(
    val name: String,
    val url: String,
    val license: String,
) {
    companion object {
        val defaultList = listOf<License>(
            License(
                "Accompanist",
                "https://github.com/google/accompanist/blob/main/LICENSE",
                "Apache License 2.0",
            ),
            License(
                "AndroidX",
                "https://developer.android.com/jetpack/androidx",
                "Apache License 2.0"
            ),
            License(
                "AndroidX DataStore",
                "https://developer.android.com/jetpack/androidx/releases/datastore",
                "Apache License 2.0"
            ),
            License(
                "AndroidX Lifecycle",
                "https://developer.android.com/jetpack/androidx/releases/lifecycle",
                "Apache License 2.0"
            ),
            License(
                "AndroidX Compose",
                "https://developer.android.com/jetpack/androidx/releases/compose",
                "Apache License 2.0"
            ),
            License(
                "AndroidX Compose Material",
                "https://developer.android.com/jetpack/androidx/releases/compose-material",
                "Apache License 2.0"
            ),
            License(
                "Coil",
                "https://github.com/coil-kt/coil/blob/main/LICENSE.txt",
                "Apache License 2.0"
            ),
            License(
                "Kotlin",
                "https://github.com/JetBrains/kotlin",
                "Apache License 2.0"
            ),
            License(
                "Android Room-Database Backup",
                "https://github.com/rafi0101/Android-Room-Database-Backup/blob/master/LICENSE",
                "MIT License"
            ),
            License(
                "ImageViewer",
                "https://github.com/jvziyaoyao/ImageViewer/blob/main/LICENSE",
                "MIT License"
            ),
            License(
                "Compose Extended Gestures",
                "https://github.com/SmartToolFactory/Compose-Extended-Gestures/blob/master/LICENSE.md",
                "Apache License 2.0"
            ),
            License(
                "Amplituda",
                "https://github.com/lincollincol/Amplituda/blob/master/LICENSE",
                "Apache License 2.0"
            ),
            License(
                "Retrofit",
                "https://github.com/square/retrofit/blob/master/LICENSE.txt",
                "Apache License 2.0"
            ),
            License(
                "Compose Destinations",
                "https://github.com/raamcosta/compose-destinations/blob/main/LICENSE.txt",
                "Apache License 2.0"
            ),
            License(
                "Gson",
                "https://github.com/google/gson/blob/master/LICENSE",
                "Apache License 2.0"
            ),
        )
    }
}