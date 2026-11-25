package com.ojhdtapp.parabox.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import com.ojhdtapp.parabox.ui.common.LayoutType

/**
 * Material Design 3 compliant navigation transitions and predictive back animations for Navigation 3.
 */
object AppNavTransitions {
    private const val ENTER_DURATION_MS = 300
    private const val EXIT_DURATION_MS = 250
    const val MENU_TAB_METADATA_KEY = "menu_tab"

    fun menuTabMetadata(tab: MenuTab): Map<String, Any> = mapOf(MENU_TAB_METADATA_KEY to tab)

    /**
     * Standard Material Shared Axis X forward transition.
     */
    fun forwardTransition(): ContentTransform =
        (slideInHorizontally(animationSpec = tween(ENTER_DURATION_MS)) { it / 5 } +
            fadeIn(animationSpec = tween(ENTER_DURATION_MS))) togetherWith
            (slideOutHorizontally(animationSpec = tween(EXIT_DURATION_MS)) { -it / 5 } +
                fadeOut(animationSpec = tween(EXIT_DURATION_MS)))

    /**
     * Standard Material Shared Axis X backward transition (used for button / 3-button nav bar pop).
     */
    fun popTransition(): ContentTransform =
        (slideInHorizontally(animationSpec = tween(ENTER_DURATION_MS)) { -it / 5 } +
            fadeIn(animationSpec = tween(ENTER_DURATION_MS))) togetherWith
            (slideOutHorizontally(animationSpec = tween(EXIT_DURATION_MS)) { it / 5 } +
                fadeOut(animationSpec = tween(EXIT_DURATION_MS)))

    /**
     * Pure cross-fade transition used for top-level tab switching on medium/expanded screens (tablets+).
     */
    fun fadeTransition(): ContentTransform =
        fadeIn(animationSpec = tween(ENTER_DURATION_MS)) togetherWith
            fadeOut(animationSpec = tween(EXIT_DURATION_MS))

    /**
     * Top-level tab transition:
     * - Compact (phone): directional horizontal slide + fade based on relative tab order.
     * - Medium/Expanded (tablet or larger): cross-fade only.
     */
    fun topLevelTabTransition(
        initialTab: MenuTab,
        targetTab: MenuTab,
        isCompactWidth: Boolean,
    ): ContentTransform {
        if (!isCompactWidth) return fadeTransition()
        return when {
            targetTab.ordinal > initialTab.ordinal -> forwardTransition()
            targetTab.ordinal < initialTab.ordinal -> popTransition()
            else -> fadeTransition()
        }
    }

    /**
     * Google Material Design 3 Predictive Back transition:
     * - Scales exiting surface down toward 0.9f while shifting gently toward the swipe direction
     *   based on [swipeEdge] (EDGE_LEFT vs EDGE_RIGHT).
     * - Smoothly scales up and fades in the previous destination underneath.
     */
    fun predictivePopTransition(swipeEdge: Int): ContentTransform {
        val direction = if (swipeEdge == NavigationEvent.EDGE_LEFT) 1 else -1
        return (scaleIn(initialScale = 0.95f) + fadeIn()) togetherWith
            (scaleOut(targetScale = 0.9f) +
                slideOutHorizontally { width -> direction * (width / 10) } +
                fadeOut())
    }
}

/**
 * Unified PaneScaffoldDirective based on Material 3 Adaptive [currentWindowAdaptiveInfo].
 * Automatically supports Compact (1 pane), Medium/Expanded (2 panes), Large/Extra-Large (3 panes),
 * and foldable hinges.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberAppPaneScaffoldDirective(): PaneScaffoldDirective {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val base = calculatePaneScaffoldDirective(adaptiveInfo)
    return base.copy(
        defaultPanePreferredWidth = 352.dp,
    )
}

/**
 * Derives [LayoutType] consistently from the active [PaneScaffoldDirective].
 */
fun PaneScaffoldDirective.toLayoutType(): LayoutType =
    if (maxHorizontalPartitions <= 1) LayoutType.NORMAL else LayoutType.SPLIT
