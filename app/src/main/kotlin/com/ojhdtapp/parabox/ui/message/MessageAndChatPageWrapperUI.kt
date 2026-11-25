package com.ojhdtapp.parabox.ui.message

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import kotlinx.coroutines.launch

/**
 * Hosts message-scoped dialogs ([EnabledChatFilterDialog] and [EditChatTagsDialog])
 * independently of the Navigation 3 list/detail/extra pane layout.
 */
@Composable
fun MessageDialogsHost(
    mainSharedViewModel: MainSharedViewModel,
    viewModel: MessagePageViewModel,
) {
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()

    EnabledChatFilterDialog(
        openDialog = state.openEnabledChatFilterDialog,
        enabledList = mainSharedState.datastore.enabledChatFilterList,
        onConfirm = {
            mainSharedViewModel.sendEvent(MainSharedEvent.UpdateEnabledChatFilterList(it))
            viewModel.sendEvent(
                MessagePageEvent.UpdateSelectedChatFilter(
                    state.selectedChatFilterLists.toMutableList().apply {
                        retainAll(it)
                        if (isEmpty()) {
                            add(ChatFilter.Normal)
                        }
                    },
                ),
            )
            viewModel.sendEvent(MessagePageEvent.OpenEnabledChatFilterDialog(false))
        },
        onDismiss = {
            viewModel.sendEvent(MessagePageEvent.OpenEnabledChatFilterDialog(false))
        },
    )

    EditChatTagsDialog(
        openDialog = state.editingChatTags != null,
        tags = state.editingChatTags?.tags ?: emptyList(),
        onConfirm = {
            coroutineScope.launch {
                state.editingChatTags?.let { editing ->
                    viewModel.sendEvent(
                        MessagePageEvent.UpdateChatTags(
                            editing.chatId,
                            it,
                            editing.tags,
                        ),
                    )
                }
            }
            viewModel.sendEvent(MessagePageEvent.UpdateEditingChatTags(null))
        },
        onDismiss = {
            viewModel.sendEvent(MessagePageEvent.UpdateEditingChatTags(null))
        },
    )
}
