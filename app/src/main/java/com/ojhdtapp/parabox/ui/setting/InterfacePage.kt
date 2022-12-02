package com.ojhdtapp.parabox.ui.setting

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.theme.Theme
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.SimpleMenuPreference
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun InterfacePage(
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
    // Dynamic Color
    val enableDynamicColor by viewModel.enableDynamicColorFlow.collectAsState(initial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
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
                title = { Text(stringResource(R.string.user_interface)) },
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
                PreferencesCategory(text = stringResource(R.string.user_interface_theme))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.user_interface_monet_title),
                    subtitleOn = stringResource(R.string.user_interface_monet_subtitle_on),
                    subtitleOff = stringResource(R.string.user_interface_monet_subtitle_off),
                    checked = enableDynamicColor,
                    onCheckedChange = viewModel::setEnableDynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                )
            }
            item {
                SimpleMenuPreference(
                    title = stringResource(R.string.user_interface_color),
                    selectedKey = viewModel.themeFlow.collectAsState(initial = Theme.WILLOW).value,
                    optionsMap = mapOf(
                        Theme.WILLOW to stringResource(R.string.color_willow),
                        Theme.PURPLE to stringResource(R.string.color_purple),
                        Theme.SAKURA to stringResource(R.string.color_sakura),
                        Theme.GARDENIA to stringResource(R.string.color_gardenia),
                        Theme.WATER to stringResource(R.string.color_water)
                    ),
                    enabled = !enableDynamicColor,
                    onSelect = viewModel::setTheme
                )
            }
            item {
                PreferencesCategory(text = stringResource(R.string.language))
            }
            item {
                SimpleMenuPreference(
                    title = stringResource(R.string.language),
                    optionsMap = mapOf("zh-rCN" to "中文（中国）", "en" to "English", "ja" to "日本語"),
                    selectedKey = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag(),
                    onSelect = {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(it)
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    })
            }
        }
    }
}