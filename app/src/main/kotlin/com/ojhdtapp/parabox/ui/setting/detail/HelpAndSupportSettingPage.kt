package com.ojhdtapp.parabox.ui.setting.detail

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.play.core.review.ReviewManagerFactory
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.BrowserUtil
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.LocalPlayAppUpdateUtil
import com.ojhdtapp.parabox.core.util.launchPlayStore
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.common.UnderConstructionPage
import com.ojhdtapp.parabox.ui.navigation.SettingRoute
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    onNavigateBack: () -> Unit,
    onNavigateTo: (SettingRoute) -> Unit,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    if (layoutType == LayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
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
                        text = "帮助与支持",
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
                    onNavigateTo = onNavigateTo,
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
                            text = "帮助与支持",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
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
                onNavigateTo = onNavigateTo,
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
    onNavigateTo: (SettingRoute) -> Unit,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) =
    LazyColumn(modifier = modifier) {
        item {
            SettingHeader(text = stringResource(R.string.ask_for_help))
        }
        item {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            SettingItem(
                title = stringResource(R.string.document_title),
                subTitle = stringResource(R.string.document_subtitle),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = "document",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                ) {
                coroutineScope.launch {
                    BrowserUtil.launchURL(
                        context = context,
                        url = "https://docs.parabox.ojhdt.dev/"
                    )
                }
            }
        }
        item {
            val context = LocalContext.current
            SettingItem(
                title = stringResource(R.string.email_title),
                subTitle = stringResource(R.string.email_subtitle),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "email",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                BrowserUtil.composeEmail(
                    context = context,
                    addresses = arrayOf("parabox@ojhdt.dev"),
                    subject = "Parabox 用户反馈"
                )
            }
        }
        item {
            SettingHeader(text = stringResource(R.string.social))
        }
        item {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            SettingItem(
                title = stringResource(R.string.web_title),
                subTitle = stringResource(R.string.web_subtitle),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Web,
                        contentDescription = "web",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                coroutineScope.launch {
                    BrowserUtil.launchURL(
                        context = context,
                        url = "https://parabox.ojhdt.dev/"
                    )
                }
            }
        }
        item {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            SettingItem(
                title = stringResource(R.string.github_title),
                subTitle = stringResource(R.string.github_subtitle),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    FaIcon(
                        modifier = Modifier.padding(end = 4.dp),
                        faIcon = FaIcons.Github, tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                coroutineScope.launch {
                    BrowserUtil.launchURL(
                        context = context,
                        url = "https://github.com/Parabox-App/Parabox"
                    )
                }
            }
        }
        item {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            SettingItem(
                title = stringResource(R.string.tg_title),
                subTitle = stringResource(R.string.tg_subtitle),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    FaIcon(
                        modifier = Modifier.padding(end = 4.dp),
                        faIcon = FaIcons.Telegram,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                coroutineScope.launch {
                    BrowserUtil.launchURL(
                        context = context,
                        url ="https://t.me/parabox_support"
                    )
                }
            }
        }
        item {
            SettingHeader(text = stringResource(R.string.support_developer))
        }
        item {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            SettingItem(
                title = stringResource(R.string.translate_title),
                subTitle = stringResource(R.string.translate_subtitle),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.GTranslate,
                        contentDescription = "translate",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                coroutineScope.launch {
                    BrowserUtil.launchURL(
                        context = context,
                        url ="https://crowdin.com/project/parabox"
                    )
                }
            }
        }
        item {
            val context = LocalContext.current as Activity
            val coroutineScope = rememberCoroutineScope()
            val reviewManager = remember {
                ReviewManagerFactory.create(context)
            }
            SettingItem(
                title = stringResource(R.string.rate_title),
                subTitle = stringResource(R.string.rate_subtitle),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.StarRate,
                        contentDescription = "rate",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                val request = reviewManager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        reviewManager.launchReviewFlow(context, reviewInfo)
                    } else {
                        task.exception?.printStackTrace()
                        coroutineScope.launch {
                            context.launchPlayStore(context.packageName)
                        }
                    }
                }
            }
        }
        item {
            SettingHeader(text = "版本信息")
        }
        item {
            val context = LocalContext.current
            val playAppUpdateUtil = LocalPlayAppUpdateUtil.current
            var showEasterEgg by remember { mutableStateOf(false) }
            var easterEggCount by remember { mutableStateOf(0) }
            var lastClickTime by remember { mutableStateOf(0L) }
            SettingItem(
                title = stringResource(R.string.version),
                subTitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "version",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                if (showEasterEgg) {
                    Toast.makeText(context, "真的没有彩蛋啦(｀д′)", Toast.LENGTH_SHORT).show()
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 500) {
                        easterEggCount++
                        if (easterEggCount >= 10) {
                            showEasterEgg = true
                        }
                    } else {
                        easterEggCount = 0
                        playAppUpdateUtil.checkAndUpdate {
                            if (!it) {
                                Toast.makeText(context, "应用为最新版本", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    lastClickTime = currentTime
                }
            }
        }
        item {
            SettingItem(
                title = "变更日志",
                subTitle = "看看新变化",
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                        contentDescription = "release note",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                onMainSharedEvent(MainSharedEvent.ShowChangeLog(true))
            }
        }
        item {
            SettingItem(
                title = stringResource(id = R.string.open_source_license),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Source,
                        contentDescription = "open source",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                onNavigateTo(SettingRoute.OpenSourceLicense)
            }
        }
    }