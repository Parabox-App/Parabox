package com.ojhdtapp.parabox.ui.common

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException

/**
 * Google Material Design 3 Predictive Back motion tokens and unified primitives.
 */
object PredictiveBackDefaults {
    /** Material 3 predictive back interpolation curve: heavy dampening, no overshoot. */
    val Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

    /** Target minimum scale for dialogs and full-screen surfaces during pre-commit. */
    const val DialogMinScale = 0.9f
    const val DialogMinAlpha = 0.72f
    val DialogMaxShift: Dp = 16.dp

    /** Target visual parameters for fullscreen media previewer during pre-commit. */
    const val MediaMinScale = 0.85f
    val MediaMaxCornerRadius: Dp = 24.dp
    const val MediaMinScrimAlpha = 0.25f

    /** Gentle resistance scale when intercepting unsaved form back navigation. */
    const val InterceptMinScale = 0.96f
    val InterceptMaxCornerRadius: Dp = 16.dp
}

/**
 * Unified Dialog host that integrates Android Predictive Back gestures into Compose Dialog windows.
 * Follows Material Design 3 guidelines: scales toward 0.9f, shifts gently in the swipe direction,
 * and smoothly springs back on cancel or completes dismissal on commit.
 */
@Composable
fun PredictiveBackDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            usePlatformDefaultWidth = properties.usePlatformDefaultWidth,
            decorFitsSystemWindows = properties.decorFitsSystemWindows,
        ),
    ) {
        PredictiveBackHandler(enabled = properties.dismissOnBackPress) { backEvents ->
            try {
                backEvents.collect { event ->
                    swipeEdge = event.swipeEdge
                    progress.snapTo(PredictiveBackDefaults.Easing.transform(event.progress))
                }
                progress.animateTo(1f, animationSpec = tween(durationMillis = 120))
                onDismissRequest()
            } catch (e: CancellationException) {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                )
            }
        }

        val currentProgress = progress.value
        val direction = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f

        Box(
            modifier = modifier.graphicsLayer {
                val scale = lerp(1f, PredictiveBackDefaults.DialogMinScale, currentProgress)
                scaleX = scale
                scaleY = scale
                translationX = direction * PredictiveBackDefaults.DialogMaxShift.toPx() * currentProgress
                alpha = lerp(1f, PredictiveBackDefaults.DialogMinAlpha, currentProgress)
            },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * Drop-in Material 3 AlertDialog with built-in Predictive Back gesture animations.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParaboxAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    PredictiveBackDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = modifier.sizeIn(minWidth = 280.dp, maxWidth = 560.dp),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(DialogPadding),
            ) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(
                            modifier = Modifier
                                .padding(IconPadding)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            it()
                        }
                    }
                }
                title?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides titleContentColor,
                        LocalTextStyle provides MaterialTheme.typography.headlineSmall,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(TitlePadding)
                                .align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally),
                        ) {
                            it()
                        }
                    }
                }
                text?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides textContentColor,
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(weight = 1f, fill = false)
                                .padding(TextPadding)
                                .align(Alignment.Start),
                        ) {
                            it()
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.End),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.primary,
                        LocalTextStyle provides MaterialTheme.typography.labelLarge,
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Container for screens that conditionally guard back navigation (e.g. unsaved changes).
 * When [enabled] is false, back gestures pass through untouched to the outer Navigation 3 host.
 * When [enabled] is true, the user gets damped physical feedback and [onIntercepted] runs on commit.
 */
@Composable
fun PredictiveBackGuardContainer(
    enabled: Boolean,
    onIntercepted: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    PredictiveBackHandler(enabled = enabled) { backEvents ->
        try {
            backEvents.collect { event ->
                swipeEdge = event.swipeEdge
                progress.snapTo(PredictiveBackDefaults.Easing.transform(event.progress))
            }
            progress.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            onIntercepted()
        } catch (e: CancellationException) {
            progress.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val currentProgress = progress.value
    val direction = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f

    Box(
        modifier = modifier.graphicsLayer {
            if (currentProgress > 0f) {
                val scale = lerp(1f, PredictiveBackDefaults.InterceptMinScale, currentProgress)
                scaleX = scale
                scaleY = scale
                translationX = direction * 12.dp.toPx() * currentProgress
                val cornerPx = lerp(0f, PredictiveBackDefaults.InterceptMaxCornerRadius.toPx(), currentProgress)
                shape = RoundedCornerShape(cornerPx)
                clip = true
            }
        },
    ) {
        content()
    }
}

private val DialogPadding = PaddingValues(all = 24.dp)
private val IconPadding = PaddingValues(bottom = 16.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)
private val TextPadding = PaddingValues(bottom = 24.dp)
