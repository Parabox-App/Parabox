package com.ojhdtapp.parabox.ui.common

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

//@ExperimentalMaterial3AdaptiveApi
//fun calculateMyStandardPaneScaffoldDirective(
//    windowSizeClass: WindowSizeClass,
//    windowAdaptiveInfo: WindowAdaptiveInfo,
//    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating
//): PaneScaffoldDirective {
//    val maxHorizontalPartitions: Int
//    val contentPadding: PaddingValues
//    val verticalSpacerSize: Dp
//    when (windowSizeClass.widthSizeClass) {
//        WindowWidthSizeClass.Compact -> {
//            maxHorizontalPartitions = 1
//            contentPadding = PaddingValues(0.dp)
//            verticalSpacerSize = 0.dp
//        }
//        WindowWidthSizeClass.Medium -> {
//            maxHorizontalPartitions = 1
//            contentPadding = PaddingValues(0.dp)
//            verticalSpacerSize = 0.dp
//        }
//        else -> {
//            maxHorizontalPartitions = 2
//            contentPadding = PaddingValues(horizontal = 24.dp)
//            verticalSpacerSize = 24.dp
//        }
//    }
//    val maxVerticalPartitions: Int
//    val horizontalSpacerSize: Dp
//
//    if (windowAdaptiveInfo.windowPosture.isTabletop) {
//        maxVerticalPartitions = 2
//        horizontalSpacerSize = 24.dp
//    } else {
//        maxVerticalPartitions = 1
//        horizontalSpacerSize = 0.dp
//    }
//
//    return PaneScaffoldDirective(
//        contentPadding,
//        maxHorizontalPartitions,
//        verticalSpacerSize,
//        maxVerticalPartitions,
//        horizontalSpacerSize,
//        getExcludedVerticalBounds(windowAdaptiveInfo.windowPosture, verticalHingePolicy)
//    )
//}
//
//@ExperimentalMaterial3AdaptiveApi
//fun calculateMyDensePaneScaffoldDirective(
//    windowSizeClass: WindowSizeClass,
//    windowAdaptiveInfo: WindowAdaptiveInfo,
//    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating
//): PaneScaffoldDirective {
//    val maxHorizontalPartitions: Int
//    val contentPadding: PaddingValues
//    val verticalSpacerSize: Dp
//    when (windowSizeClass.widthSizeClass) {
//        WindowWidthSizeClass.Compact -> {
//            maxHorizontalPartitions = 1
//            contentPadding = PaddingValues(0.dp)
//            verticalSpacerSize = 0.dp
//        }
//        WindowWidthSizeClass.Medium -> {
//            maxHorizontalPartitions = 2
//            contentPadding = PaddingValues(horizontal = 16.dp)
//            verticalSpacerSize = 16.dp
//        }
//        else -> {
//            maxHorizontalPartitions = 2
//            contentPadding = PaddingValues(horizontal = 24.dp)
//            verticalSpacerSize = 24.dp
//        }
//    }
//    val maxVerticalPartitions: Int
//    val horizontalSpacerSize: Dp
//
//    if (windowAdaptiveInfo.windowPosture.isTabletop) {
//        maxVerticalPartitions = 2
//        horizontalSpacerSize = 24.dp
//    } else {
//        maxVerticalPartitions = 1
//        horizontalSpacerSize = 0.dp
//    }
//
//    return PaneScaffoldDirective(
//        contentPadding,
//        maxHorizontalPartitions,
//        verticalSpacerSize,
//        maxVerticalPartitions,
//        horizontalSpacerSize,
//        getExcludedVerticalBounds(windowAdaptiveInfo.windowPosture, verticalHingePolicy)
//    )
//}
//
//@OptIn(ExperimentalMaterial3AdaptiveApi::class)
//private fun getExcludedVerticalBounds(posture: Posture, hingePolicy: HingePolicy): List<Rect> {
//    return when (hingePolicy) {
//        HingePolicy.AvoidSeparating -> posture.separatingVerticalHingeBounds
//        HingePolicy.AvoidOccluding -> posture.occludingVerticalHingeBounds
//        HingePolicy.AlwaysAvoid -> posture.allVerticalHingeBounds
//        else -> emptyList()
//    }
//}