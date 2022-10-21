package com.ojhdtapp.parabox.ui.file

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    workInfoMap: Map<String, Pair<File, List<WorkInfo>>>,
    onCancel: (fileId: Long) -> Unit,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        val workInfoPlainList by remember{
            derivedStateOf {
                workInfoMap.toList()
            }
        }
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
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        items(items = workInfoPlainList, key = {it.first}) {
                            val workInfoList by remember(workInfoPlainList) {
                                derivedStateOf {
                                    it.second.second
                                }
                            }
                            WorkInfoItem(
                                file = it.second.first,
                                workInfoList = workInfoList,
                                onClick = {
                                    onCancel(it.second.first.fileId)
                                }
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
    val cancelable by remember(workInfoList) {
        derivedStateOf {
            !workInfoList.any { it.state == WorkInfo.State.CANCELLED }
                    && !workInfoList.all { it.state == WorkInfo.State.SUCCEEDED }
        }
    }
    val des = remember(workInfoList) {
        derivedStateOf {
            when {
                workInfoList.any {
                    it.state == WorkInfo.State.CANCELLED
                } -> "已取消"

                workInfoList.any {
                    it.state == WorkInfo.State.FAILED
                } -> {
                    var count = 0
                    workInfoList.forEach { count += it.runAttemptCount }
                    "操作失败(第${count}次尝试)"
                }

                workInfoList.all {
                    it.state == WorkInfo.State.SUCCEEDED
                } -> "已完成"

                workInfoList.all {
                    it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED
                } -> "已暂停"

                workInfoList.any {
                    it.state == WorkInfo.State.RUNNING
                } -> {
                    val currentStep =
                        workInfoList.count { it.state == WorkInfo.State.SUCCEEDED } + 1
                    when (currentStep) {
                        1 -> "正在下载($currentStep/3)"
                        2 -> "正在上传($currentStep/3)"
                        3 -> "正在清理($currentStep/3)"
                        else -> "正在执行"
                    }
                }

                else -> "未知进度"
            }

        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Text(
                text = des.value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onClick, enabled = cancelable) {
            Text(text = "取消")
        }
    }
}