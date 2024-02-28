package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

fun Modifier.placeholder(
    isLoading: Boolean,
    backgroundColor: Color = Color.Unspecified,
    shape: Shape = RoundedCornerShape(4.dp),
    showShimmerAnimation: Boolean = true
): Modifier = composed {
    val highlight = if (showShimmerAnimation) {
        PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.onTertiary)
    } else {
        null
    }
    val specifiedBackgroundColor = backgroundColor.takeOrElse { Color(0xFFDBD6D1).copy(0.6f) }
    Modifier.placeholder(
        color = specifiedBackgroundColor,
        visible = isLoading,
        shape = shape,
        highlight = highlight
    )
}