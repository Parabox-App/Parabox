package com.ojhdtapp.parabox.ui.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.BrowserUtil
import com.ojhdtapp.parabox.core.util.launchNotificationSetting
import com.ojhdtapp.parabox.core.util.launchPlayStore
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.LicensePageDestination
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun SupportPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    AgreementDialog(
        showDialog = viewModel.showTermsDialog.value,
        icon = {
            Icon(imageVector = Icons.Outlined.Gavel, contentDescription = "terms")
        },
        title = "用户协议",
        contentResId = R.string.terms,
        onConfirm = {
            viewModel.setShowTermsDialog(false)
        },
        onDismiss = {
            viewModel.setShowTermsDialog(false)
        },
    )

    AgreementDialog(
        showDialog = viewModel.showPrivacyDialog.value,
        icon = {
            Icon(imageVector = Icons.Outlined.PrivacyTip, contentDescription = "privacy")
        },
        title = "隐私政策",
        contentResId = R.string.privacy,
        onConfirm = {
            viewModel.setShowPrivacyDialog(false)
        },
        onDismiss = {
            viewModel.setShowPrivacyDialog(false)
        },
    )

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
                title = { Text("帮助与支持") },
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
                PreferencesCategory(text = "寻求帮助")
            }
            item {
                NormalPreference(
                    title = "文档中心",
                    subtitle = "阅读针对用户和开发者的问题说明",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = "document"
                        )
                    },
                    onClick = {
                        BrowserUtil.launchURL(
                            context = context,
                            url = "https://docs.parabox.ojhdt.dev/"
                        )
                    }
                )
            }
            item {
                NormalPreference(
                    title = "发送反馈邮件",
                    subtitle = "发送您的问题或建议",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "email"
                        )
                    },
                    onClick = {
                        BrowserUtil.composeEmail(
                            context = context,
                            addresses = arrayOf("ojhdtmail@gmail.com"),
                            subject = "Parabox 用户反馈"
                        )
                    }
                )
            }
            item {
                PreferencesCategory(text = "社交")
            }
            item {
                NormalPreference(
                    title = "网站",
                    subtitle = "查看我们的网站",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Web,
                            contentDescription = "web"
                        )
                    },
                    onClick = {
                        BrowserUtil.launchURL(context = context, url = "https://parabox.ojhdt.dev/")
                    }
                )
            }
            item {
                NormalPreference(
                    title = "Github",
                    subtitle = "星标，创建克隆，发起提案",
                    leadingIcon = {
                        FaIcon(
                            modifier = Modifier.padding(end = 4.dp),
                            faIcon = FaIcons.Github, tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        BrowserUtil.launchURL(
                            context = context,
                            url = "https://github.com/Parabox-App/Parabox"
                        )
                    }
                )
            }
            item {
                NormalPreference(
                    title = "加入 Telegram 群组",
                    subtitle = "获取最新动态，提出建议，讨论错误",
                    leadingIcon = {
                        FaIcon(
                            modifier = Modifier.padding(end = 4.dp),
                            faIcon = FaIcons.Telegram,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        BrowserUtil.launchURL(
                            context = context,
                            url = "https://t.me/parabox_support"
                        )
                    }
                )
            }
            item {
                PreferencesCategory(text = "支持开发者")
            }
            item {
                NormalPreference(
                    title = "贡献翻译",
                    subtitle = "帮助我们将应用翻译成您的语言",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.GTranslate,
                            contentDescription = "translate"
                        )
                    },
                    onClick = {

                    }
                )
            }
            item {
                NormalPreference(
                    title = "为该应用评分",
                    subtitle = "喜欢这个应用吗？前往 Google Play 给出您的评价",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.StarRate,
                            contentDescription = "rate"
                        )
                    },
                    onClick = {
                        context.launchPlayStore(context.packageName)
                    }
                )
            }
            item {
                NormalPreference(
                    title = "捐赠",
                    subtitle = "支持这个项目走得更远",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Cake,
                            contentDescription = "donate"
                        )
                    },
                    onClick = {
                    }
                )
            }
            item {
                PreferencesCategory(text = "协议与条款")
            }
            item {
                NormalPreference(
                    title = "用户协议",
                    onClick = {
                        viewModel.setShowTermsDialog(true)
                    }
                )
            }
            item {
                NormalPreference(
                    title = "隐私政策",
                    onClick = {
                        viewModel.setShowPrivacyDialog(true)
                    }
                )
            }
            item {
                NormalPreference(
                    title = "开放源代码许可",
                    onClick = {
                        mainNavController.navigate(LicensePageDestination)
                    }
                )
            }
        }
    }
}