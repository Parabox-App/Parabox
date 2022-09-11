package com.ojhdtapp.parabox.ui.file

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.use_case.GetFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePageViewModel @Inject constructor(
    val getFiles: GetFiles
) : ViewModel() {
    init {
        onSearch("", withoutDelay = true)
    }

    private val _uiEventFlow = MutableSharedFlow<FilePageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val _searchText = mutableStateOf<String>("")
    val searchText: State<String> = _searchText

    private var searchJob: Job? = null
    fun onSearch(value: String, withoutDelay: Boolean = false) {
        _searchText.value = value
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            if (!withoutDelay) {
                delay(800L)
                getFiles(value)
                    .onEach { result ->
                        when (result) {
                            is Resource.Success -> {
                                _fileStateFlow.value = _fileStateFlow.value.copy(
                                    isLoading = false,
                                    data = result.data ?: emptyList()
                                )
                            }
                            is Resource.Loading -> {
                                _fileStateFlow.value = _fileStateFlow.value.copy(
                                    isLoading = true
                                )
                            }
                            is Resource.Error -> {
                                _fileStateFlow.value = _fileStateFlow.value.copy(
                                    isLoading = false,
                                    data = result.data ?: emptyList()
                                )
                                _uiEventFlow.emit(
                                    FilePageUiEvent.ShowSnackBar(
                                        result.message ?: "未知错误"
                                    )
                                )
                            }
                        }
                    }
            }
        }
    }

    private var _fileStateFlow = MutableStateFlow(FilePageState())
    val fileStateFlow get() = _fileStateFlow.asStateFlow()

}