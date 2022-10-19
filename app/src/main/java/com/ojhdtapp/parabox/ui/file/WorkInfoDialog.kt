package com.ojhdtapp.parabox.ui.file

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.work.WorkInfo
import com.ojhdtapp.parabox.domain.model.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WorkInfoDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    workInfoMap: Map<File, List<WorkInfo>>,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            val horizontalPadding = when (sizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 16.dp
                WindowWidthSizeClass.Medium -> 32.dp
                WindowWidthSizeClass.Expanded -> 0.dp
                else -> 16.dp
            }
            Surface(
                modifier = modifier
                    .widthIn(0.dp, 580.dp)
                    .padding(horizontal = horizontalPadding)
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "备份中的任务",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Text(
                        text = "以下任务已被加入处理队列。处理过程接受系统调度，将尽可能减少资源占用与性能消耗。",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        item {
                            if (workInfoMap.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(176.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "暂无任务")
                                }
                            }
                        }
                        items(items = workInfoMap.toList(), key = { it.first.fileId }) {
                            WorkInfoItem(
                                file = it.first,
                                workInfoList = it.second,
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkInfoItem(
    modifier: Modifier = Modifier,
    file: File,
    workInfoList: List<WorkInfo>,
    onClick: () -> Unit
) {
    val currentStep = remember {
        derivedStateOf {
            workInfoList.count { it.state == WorkInfo.State.SUCCEEDED }
        }
    }
    val currentStepDes = remember {
        derivedStateOf {
            when (currentStep.value) {
                0 -> "正在下载(0/3)"
                1 -> "正在上传(1/3)"
                2 -> "正在清理(2/3)"
                else -> "已完成"
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = currentStepDes.value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = { /*TODO*/ }, enabled = true) {
            Text(text = "取消")
        }
    }
}