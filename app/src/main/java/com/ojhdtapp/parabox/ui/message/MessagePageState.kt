package com.ojhdtapp.parabox.ui.message

data class MessagePageState(
    val enabledGetChatFilterList: List<GetChatFilter> = emptyList(),
    val selectedGetChatFilterList: List<GetChatFilter> = listOf(GetChatFilter.Normal),
    val datastore: DataStore = DataStore(),
    val openEnabledChatFilterDialog: Boolean = false,
){
    data class DataStore(
        val enableSwipeToDismiss: Boolean = false
    )
}