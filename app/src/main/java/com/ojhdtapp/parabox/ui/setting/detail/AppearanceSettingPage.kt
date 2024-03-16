package com.ojhdtapp.parabox.ui.setting.detail

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.os.LocaleListCompat
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingLayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.parabox.ui.theme.Theme
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppearanceSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: SettingLayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    BackHandler(enabled = layoutType != SettingLayoutType.SPLIT) {
        scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
    }
    if (layoutType == SettingLayoutType.SPLIT) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        text = "界面",
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
                            text = "界面",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest) }) {
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
    layoutType: SettingLayoutType,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) =
    LazyColumn(modifier = modifier) {
        item {
            SettingHeader(text = "主题")
        }
        item {
            SettingItem(
                title = stringResource(R.string.user_interface_monet_title),
                subTitle = stringResource(R.string.user_interface_monet_subtitle_off),
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.enableDynamicColor, onCheckedChange = {
                        onMainSharedEvent(
                            MainSharedEvent.UpdateSettingSwitch(
                                DataStoreKeys.SETTINGS_ENABLE_DYNAMIC_COLOR,
                                it
                            )
                        )
                    })
                }) {
                onMainSharedEvent(
                    MainSharedEvent.UpdateSettingSwitch(
                        DataStoreKeys.SETTINGS_ENABLE_DYNAMIC_COLOR,
                        !mainSharedState.datastore.enableDynamicColor
                    )
                )
            }
        }
        item {
            AnimatedVisibility(
                visible = !mainSharedState.datastore.enableDynamicColor,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box {
                    var isMenuVisible by remember {
                        mutableStateOf(false)
                    }
                    CascadeDropdownMenu(
                        expanded = isMenuVisible,
                        onDismissRequest = { isMenuVisible = false },
                        offset = DpOffset(16.dp, 0.dp),
                        properties = PopupProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                            focusable = true
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Theme.entries.forEach {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(id = it.nameResId)) },
                                onClick = {
                                    onMainSharedEvent(
                                        MainSharedEvent.UpdateSettingMenu(
                                            DataStoreKeys.SETTINGS_THEME,
                                            it.ordinal
                                        )
                                    )
                                    isMenuVisible = false
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(it.primaryColor)
                                    )
                                }
                            )
                        }
                    }
                    SettingItem(
                        title = stringResource(id = R.string.user_interface_color),
                        subTitle = stringResource(id = mainSharedState.datastore.theme.nameResId),
                        selected = false,
                        layoutType = layoutType,
                    ) {
                        isMenuVisible = true
                    }
                }
            }
        }
        item {
            Box {
                var isMenuVisible by remember {
                    mutableStateOf(false)
                }
                CascadeDropdownMenu(
                    expanded = isMenuVisible,
                    onDismissRequest = { isMenuVisible = false },
                    offset = DpOffset(16.dp, 0.dp),
                    properties = PopupProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        focusable = true
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    DataStoreKeys.DarkMode.entries.forEach {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(id = it.nameResId)) },
                            onClick = {
                                onMainSharedEvent(
                                    MainSharedEvent.UpdateSettingMenu(
                                        DataStoreKeys.SETTINGS_DARK_MODE,
                                        it.ordinal
                                    )
                                )
                                isMenuVisible = false
                            }
                        )
                    }
                }
                SettingItem(
                    title = stringResource(id = R.string.darkmode),
                    subTitle = stringResource(id = mainSharedState.datastore.darkMode.nameResId),
                    selected = false,
                    layoutType = layoutType,
                ) {
                    isMenuVisible = true
                }
            }
        }
        item {
            SettingHeader(text = "语言")
        }
        item {
            Box {
                val context = LocalContext.current
                val languageSelectionMap = remember {
                    mapOf("zh-Hans" to "简体中文", "en" to "English", "ja" to "日本語")
//                    zh-Hans-CN
//                    en-US
//                    ja-JP
                }
                var isMenuVisible by remember {
                    mutableStateOf(false)
                }
                CascadeDropdownMenu(
                    expanded = isMenuVisible,
                    onDismissRequest = { isMenuVisible = false },
                    offset = DpOffset(16.dp, 0.dp),
                    properties = PopupProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        focusable = true
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("系统默认设置") },
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                context.getSystemService(LocaleManager::class.java).applicationLocales =
                                    LocaleList.getEmptyLocaleList()
                            } else {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.getEmptyLocaleList()
                                )
                            }
                            isMenuVisible = false
                        }
                    )
                    languageSelectionMap.forEach {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(it.value) },
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    context.getSystemService(LocaleManager::class.java).applicationLocales =
                                        LocaleList.forLanguageTags(it.key)
                                } else {
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(it.key)
                                    )
                                }
                                isMenuVisible = false
                            }
                        )
                    }
                }
                SettingItem(
                    title = stringResource(id = R.string.language),
                    subTitle = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()?.let {
                        Log.d("AppearanceSettingPage", "languageTag: $it")
                        languageSelectionMap.firstNotNullOfOrNull { entry ->
                            if (it.contains(entry.key)) entry.value else null
                        }
                    } ?: "系统默认设置",
                    selected = false,
                    layoutType = layoutType,
                ) {
                    isMenuVisible = true
                }
            }
        }
    }