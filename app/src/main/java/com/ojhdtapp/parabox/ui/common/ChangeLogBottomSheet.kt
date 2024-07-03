package com.ojhdtapp.parabox.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.BasicRichText
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.ChangeLog
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLogBottomSheet(
    modifier: Modifier = Modifier,
    changeLogBottomSheetState: MainSharedState.ChangeLogBottomSheetState,
    onEvent: (MainSharedEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    if (changeLogBottomSheetState.showBottomSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                onEvent(MainSharedEvent.ShowChangeLog(false))
            },
            modifier = modifier
        ) {
            when (changeLogBottomSheetState.loadState) {
                LoadState.LOADING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                LoadState.ERROR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Text(text = "发生错误")
                    }
                }
                LoadState.SUCCESS -> {
                    LazyColumn {
                        items(items = changeLogBottomSheetState.data) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Box(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = it.version,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                BasicRichText(modifier = Modifier.fillMaxWidth()) {
                                    Markdown(
                                        content = (AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag() ?: Locale.getDefault().language).let { tag ->
                                            if (tag.contains("zh")) {
                                                it.contentMdCn
                                            } else {
                                                it.contentMdEn
                                            }
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}