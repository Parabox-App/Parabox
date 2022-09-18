package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.ui.util.SegmentedControl

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NewContactBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState,
    sizeClass: WindowSizeClass,
    content: (@Composable () -> Unit)
) = ModalBottomSheetLayout(
    modifier = modifier,
    sheetState = sheetState,
//    sheetShape = RoundedCornerShape(24.dp),
    sheetBackgroundColor = Color.Transparent,
    sheetElevation = 0.dp,
    sheetContent = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .width(640.dp)
                        .padding(horizontal = 32.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape,
                        tonalElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.size(32.dp, 4.dp))
                    }
                    Text(text = "发起会话", style = MaterialTheme.typography.headlineSmall)
                    SegmentedControl(items = listOf("私人会话","群聊")){

                    }
                    Spacer(modifier = Modifier.height(200.dp))
                    Row(modifier = Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        Button(onClick = { /*TODO*/ }, enabled = false) {
                            Text(text = "确定")
                        }
                    }
                }
            }
        }
    }) {
    content()
}