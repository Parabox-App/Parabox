import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.allVerticalHingeBounds
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.occludingVerticalHingeBounds
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

/**
 * Calculates the standard [PaneScaffoldDirective] from a given [WindowAdaptiveInfo]. Use this
 * method with [currentWindowAdaptiveInfo] to acquire Material-recommended adaptive layout
 * settings of the current activity window.
 *
 * See more details on the [Material design guideline site]
 * (https://m3.material.io/foundations/layout/applying-layout/window-size-classes).
 *
 * @param windowAdaptiveInfo [WindowAdaptiveInfo] that collects useful information in making
 *        layout adaptation decisions like [WindowSizeClass].
 * @param verticalHingePolicy [HingePolicy] that decides how layouts are supposed to address
 *        vertical hinges.
 * @return an [PaneScaffoldDirective] to be used to decide adaptive layout states.
 */
// TODO(b/285144647): Add more details regarding the use scenarios of this function.
@ExperimentalMaterial3AdaptiveApi
fun calculateMyPaneScaffoldDirective(
    windowSizeClass: WindowSizeClass,
    windowAdaptiveInfo: WindowAdaptiveInfo,
    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating
): PaneScaffoldDirective {
    val maxHorizontalPartitions: Int
    val contentPadding: PaddingValues
    val verticalSpacerSize: Dp
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            maxHorizontalPartitions = 1
            contentPadding = PaddingValues(0.dp)
            verticalSpacerSize = 0.dp
        }
        WindowWidthSizeClass.Medium -> {
            maxHorizontalPartitions = 1
            contentPadding = PaddingValues(0.dp)
            verticalSpacerSize = 0.dp
        }
        else -> {
            maxHorizontalPartitions = 2
            contentPadding = PaddingValues(24.dp)
            verticalSpacerSize = 24.dp
        }
    }
    val maxVerticalPartitions: Int
    val horizontalSpacerSize: Dp

    if (windowAdaptiveInfo.windowPosture.isTabletop) {
        maxVerticalPartitions = 2
        horizontalSpacerSize = 24.dp
    } else {
        maxVerticalPartitions = 1
        horizontalSpacerSize = 0.dp
    }

    return PaneScaffoldDirective(
        contentPadding,
        maxHorizontalPartitions,
        verticalSpacerSize,
        maxVerticalPartitions,
        horizontalSpacerSize,
        getExcludedVerticalBounds(windowAdaptiveInfo.windowPosture, verticalHingePolicy)
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun getExcludedVerticalBounds(posture: Posture, hingePolicy: HingePolicy): List<Rect> {
    return when (hingePolicy) {
        HingePolicy.AvoidSeparating -> posture.separatingVerticalHingeBounds
        HingePolicy.AvoidOccluding -> posture.occludingVerticalHingeBounds
        HingePolicy.AlwaysAvoid -> posture.allVerticalHingeBounds
        else -> emptyList()
    }
}