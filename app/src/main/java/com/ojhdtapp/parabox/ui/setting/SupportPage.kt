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
import androidx.compose.ui.res.stringResource
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
        title = stringResource(R.string.terms),
        contentResId = R.string.terms_content,
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
        title = stringResource(R.string.privacy),
        contentResId = R.string.privacy_content,
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
                title = { Text(stringResource(R.string.support)) },
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
                PreferencesCategory(text = stringResource(R.string.ask_for_help))
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.document_title),
                    subtitle = stringResource(R.string.document_subtitle),
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
                    title = stringResource(R.string.email_title),
                    subtitle = stringResource(R.string.email_subtitle),
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
                PreferencesCategory(text = stringResource(R.string.social))
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.web_title),
                    subtitle = stringResource(R.string.web_subtitle),
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
                    title = stringResource(R.string.github_title),
                    subtitle = stringResource(R.string.github_subtitle),
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
                    title = stringResource(R.string.tg_title),
                    subtitle = stringResource(R.string.tg_subtitle),
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
                PreferencesCategory(text = stringResource(R.string.support_developer))
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.translate_title),
                    subtitle = stringResource(R.string.translate_subtitle),
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
                    title = stringResource(R.string.rate_title),
                    subtitle = stringResource(R.string.rate_subtitle),
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
                    title = stringResource(R.string.donate_title),
                    subtitle = stringResource(R.string.donate_subtitle),
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
                PreferencesCategory(text = stringResource(R.string.agreement))
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.terms),
                    onClick = {
                        viewModel.setShowTermsDialog(true)
                    }
                )
            }
            item {
                NormalPreference(
                    title = stringResource(id = R.string.privacy),
                    onClick = {
                        viewModel.setShowPrivacyDialog(true)
                    }
                )
            }
            item {
                NormalPreference(
                    title = stringResource(id = R.string.open_source_license),
                    onClick = {
                        mainNavController.navigate(LicensePageDestination)
                    }
                )
            }
        }
    }
}