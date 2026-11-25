package com.ojhdtapp.parabox.ui.navigation.suite

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.menu.MenuPageEvent
import com.ojhdtapp.parabox.ui.navigation.MenuTab

private val MinimumExpandedItemHeight = 56.dp
private val CollapsedItemHeight = 48.dp
private val CapsulePadding = 4.dp
private val BottomMargin = 12.dp

@Composable
private fun tabLabelHeight(): Dp = with(LocalDensity.current) {
    MaterialTheme.typography.labelSmall.lineHeight.toDp()
}

private fun expandedItemHeight(labelHeight: Dp) = maxOf(MinimumExpandedItemHeight, labelHeight + 32.dp)

@Composable
internal fun floatingBottomBarClearance(): Dp =
    expandedItemHeight(tabLabelHeight()) + CapsulePadding * 2 + BottomMargin

internal val LocalBottomBarClearance = staticCompositionLocalOf { 0.dp }

internal fun PaddingValues.withBottomBarClearance(clearance: Dp): PaddingValues {
    if (clearance == 0.dp) return this
    val insets = this
    return object : PaddingValues by insets {
        override fun calculateBottomPadding() = insets.calculateBottomPadding() + clearance
    }
}

@Composable
internal fun FloatingMenuNavigationBar(
    currentTab: MenuTab,
    unreadCount: Int,
    collapsed: Boolean,
    enabled: Boolean,
    onSelectTab: (MenuTab) -> Unit,
    onEvent: (MenuPageEvent) -> Unit,
) {
    val collapseProgress by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "bottom navigation collapse",
    )
    val labelHeight = tabLabelHeight()
    val expandedHeight = expandedItemHeight(labelHeight)
    val itemHeight = lerp(expandedHeight, CollapsedItemHeight, collapseProgress)
    val groupGap = 8.dp
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(bottom = BottomMargin),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val tabCount = MenuTab.entries.size
        val minimumTarget = 48.dp
        val capsuleGutters = CapsulePadding * 2 + groupGap
        val expandedActionSize = minOf(
            expandedHeight + CapsulePadding * 2,
            (maxWidth - minimumTarget * tabCount - capsuleGutters).coerceAtLeast(minimumTarget),
        )
        val actionSize = lerp(
            expandedActionSize,
            minOf(expandedActionSize, CollapsedItemHeight + CapsulePadding * 2),
            collapseProgress,
        )
        val extraWidth = maxWidth - expandedActionSize - capsuleGutters
        val sideMargin = ((extraWidth - minimumTarget * tabCount) / 2).coerceIn(0.dp, 12.dp)
        val expandedWidth = ((extraWidth - sideMargin * 2) / tabCount).coerceIn(minimumTarget, 76.dp)
        val itemWidth = lerp(expandedWidth, minOf(expandedWidth, 56.dp), collapseProgress)

        Row(
            modifier = Modifier.padding(horizontal = sideMargin),
            horizontalArrangement = Arrangement.spacedBy(groupGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceContainerHigh,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(CapsulePadding).selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MenuTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        FloatingMenuTab(
                            modifier = Modifier.width(itemWidth).height(itemHeight),
                            title = stringResource(tab.labelResId()),
                            icon = tab.icon(selected),
                            selected = selected,
                            enabled = enabled,
                            labelHeight = labelHeight,
                            labelVisibility = 1f - collapseProgress,
                            unreadCount = if (tab == MenuTab.Message) unreadCount else 0,
                            onClick = {
                                onEvent(MenuPageEvent.OnBarItemClicked(selected))
                                onSelectTab(tab)
                            },
                        )
                    }
                }
            }

            Surface(
                onClick = { onEvent(MenuPageEvent.OnFABClicked) },
                modifier = Modifier.size(actionSize).semantics { role = Role.Button },
                enabled = enabled,
                shape = CircleShape,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.new_contact),
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingMenuTab(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    labelHeight: Dp,
    labelVisibility: Float,
    unreadCount: Int,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant

    val unreadDescription = if (unreadCount > 0) {
        "${stringResource(R.string.unread)}: $unreadCount"
    } else {
        null
    }

    Box(
        modifier = modifier.background(
            color = if (selected) colors.secondaryContainer else Color.Transparent,
            shape = CircleShape,
        ),
    ) {
        // Clip the interaction layer only, so the unread badge can overhang the tab.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
                .semantics {
                    contentDescription = title
                    unreadDescription?.let { stateDescription = it }
                },
        )
        Column(
            modifier = Modifier.matchParentSize().clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BadgedBox(badge = {
                if (unreadCount > 0) {
                    Badge(containerColor = colors.primary, contentColor = colors.onPrimary) {
                        Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                    }
                }
            }) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            }

            if (labelVisibility > 0f) {
                Box(Modifier.height(labelHeight * labelVisibility).clipToBounds()) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .wrapContentHeight(Alignment.Top, unbounded = true)
                            .graphicsLayer { alpha = labelVisibility }
                            .clearAndSetSemantics {},
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
