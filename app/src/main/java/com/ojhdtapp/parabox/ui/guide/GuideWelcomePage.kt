package com.ojhdtapp.parabox.ui.guide

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.GuideNavGraph
import com.ojhdtapp.parabox.ui.common.NormalPreference
import com.ramcosta.composedestinations.annotation.Destination
import java.lang.Math.*
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination
@GuideNavGraph(start = true)
@Composable
fun GuideWelcomePage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
) {
    val context = LocalContext.current

    val selectedLanguage by remember {
        mutableStateOf(
            (AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }
                ?: LocaleManagerCompat.getSystemLocales(context))[0]?.displayLanguage ?: "English"
//            when(AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()?:"en"){
//                "zh-rCN" -> "中文（中国）"
//                "en" -> "English"
//                "ja" -> "日本語"
//                else -> "English"
//            }
        )

    }

    var showSkipGuideDialog by remember {
        mutableStateOf(false)
    }

    if (showSkipGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSkipGuideDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSkipGuideDialog = false
                }) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSkipGuideDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = stringResource(R.string.skip_guide_title))
            },
            text = {
                Text(text = stringResource(R.string.skip_guide_text))
            }
        )
    }

    var showLanguageDialog by remember {
        mutableStateOf(false)
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            confirmButton = {},
            text = {
                Column {
                    NormalPreference(title = "中文（中国）") {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("zh-CN")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        showLanguageDialog = false
                    }
                    NormalPreference(title = "English") {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        showLanguageDialog = false
                    }
                    NormalPreference(title = "日本語") {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ja")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        showLanguageDialog = false
                    }
                }
            }
        )
    }

    Surface(color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val infiniteTransition = rememberInfiniteTransition()
        val color by infiniteTransition.animateColor(
            initialValue = MaterialTheme.colorScheme.secondary,
            targetValue = MaterialTheme.colorScheme.tertiary,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val radius by infiniteTransition.animateFloat(
            initialValue = 100f,
            targetValue = 150f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                repeatMode = RepeatMode.Reverse
            )
        )
        val degree by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        val relativeX by infiniteTransition.animateFloat(
            initialValue = -200f,
            targetValue = 200f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                repeatMode = RepeatMode.Reverse
            )
        )
        val relativeY by infiniteTransition.animateFloat(
            initialValue = 200f,
            targetValue = -200f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                repeatMode = RepeatMode.Reverse
            )
        )

        Canvas(modifier = Modifier.fillMaxSize(), onDraw = {
            rotate(degrees = degree) {
                val centerX = this.size.width / 2 + relativeX
                val centerY = this.size.height / 2 + relativeY
                val path = Path()
                repeat(360) {
                    val theta = it * PI / 180
                    val r = (sin(20 * theta) + 20).pow(0.5)
                    val x = centerX + r * radius * cos(theta)
                    val y = centerY + r * radius * sin(theta)
                    if (it == 0) {
                        path.moveTo(x.toFloat(), y.toFloat())
                    } else {
                        path.quadraticBezierTo(x.toFloat(), y.toFloat(), x.toFloat(), y.toFloat())
                    }
                }
                drawPath(
                    path = path, color = color, style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Square
                    )
                )
            }
        })
        Column(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(id = R.string.main_welcome),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            NormalPreference(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .widthIn(
                        0.dp,
                        if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 480.dp else 3000.dp
                    ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = "set language"
                    )
                },
                title = selectedLanguage,
                roundedCorner = true,
            ) {
                showLanguageDialog = true
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    showSkipGuideDialog = true
                }) {
                    Text(text = stringResource(id = R.string.skip_guide_title))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                }) {
                    Text(text = stringResource(R.string.cont))
                }
            }
        }
    }
}
