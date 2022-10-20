package com.ojhdtapp.parabox.ui.file

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.domain.use_case.DeleteFile
import com.ojhdtapp.parabox.domain.use_case.GetFiles
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getFiles: GetFiles,
    val deleteFile: DeleteFile
) : ViewModel() {

    private val _uiEventFlow = MutableSharedFlow<FilePageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private var _fileStateFlow = MutableStateFlow(FilePageState())
    val fileStateFlow get() = _fileStateFlow.asStateFlow()

    private var _selectedFilesId: SnapshotStateList<Long> = mutableStateListOf<Long>()
    val selectedFilesId get() = _selectedFilesId
    fun clearSelectedFiles() {
        _selectedFilesId.clear()
    }

    fun addOrRemoveItemOfSelectedFileList(value: Long) {
        if (!_selectedFilesId.contains(value)) {
            _selectedFilesId.add(value)
        } else {
            _selectedFilesId.remove(value)
        }
    }

    fun deleteSelectedFile() {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedFilesId.forEach {
                deleteFile(it)
            }
            _selectedFilesId.clear()
            setSearchBarActivateState(SearchAppBar.NONE)
        }
    }

    // Google Drive Files
    private val googleDriveFilesStateFlow =
        MutableStateFlow<Resource<List<File>>>(Resource.Loading())
    fun updateGoogleDriveFilesStateFlow() {
        viewModelScope.launch {
            context.dataStore.data.first().get(DataStoreKeys.GOOGLE_WORK_FOLDER_ID)?.let {
                GoogleDriveUtil.getFileList(context = context, it)?.map {
                    File(
                        url = it.webContentLink,
                        name = it.name,
                        extension = it.fullFileExtension ?: FileUtil.getExtension(it.name),
                        size = it.getSize(),
                        timestamp = it.createdTime.value,
                        profileName = "Google Drive",
                        fileId = it.createdTime.value,
                        cloudType = GoogleDriveUtil.SERVICE_CODE,
                        cloudId = it.id
                    )
                }.also {
                    googleDriveFilesStateFlow.emit(
                        Resource.Success(it)
                    )
                    setIsRefreshing(false)
                }
            }
        }
    }
    // Swipe Refresh
    private val _isRefreshing = mutableStateOf<Boolean>(false)
    val isRefreshing: State<Boolean> = _isRefreshing
    fun setIsRefreshing(value: Boolean) {
        _isRefreshing.value = value
    }

    // Search
    private val _searchBarActivateState = mutableStateOf<Int>(SearchAppBar.NONE)
    val searchBarActivateState: State<Int> = _searchBarActivateState
    fun setSearchBarActivateState(value: Int) {
        _searchBarActivateState.value = value
    }

    private val _searchText = mutableStateOf<String>("")
    val searchText: State<String> = _searchText

    private var searchJob: Job? = null
    fun onSearch(value: String, withoutDelay: Boolean = false) {
        _searchText.value = value
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            if (!withoutDelay) delay(800L)
            getFiles(value)
                .combine(googleDriveFilesStateFlow) { local, gd ->
                    val combinedData =
                        (local.data?.toMutableList() ?: mutableListOf<File>()).apply {
                            addAll(gd.data?.toMutableList()?.apply {
                                removeAll {
                                    it.cloudId in (local.data?.map { it.cloudId } ?: emptyList())
                                }
                            } ?: emptyList())
                        }
                    when {
                        listOf(local, gd).any { it is Resource.Error } -> {
                            Resource.Error(
                                listOf(local, gd).firstOrNull() { it is Resource.Error }?.message
                                    ?: "error",
                                combinedData
                            )
                        }

                        listOf(local, gd).any { it is Resource.Success } -> {
                            Resource.Success(combinedData)
                        }

                        else -> Resource.Loading(combinedData)
                    }
                }
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
                }.launchIn(this)

        }
    }

    fun setArea(area: Int) {
        _fileStateFlow.value = _fileStateFlow.value.copy(
            area = area
        )
    }

    fun setRecentFilter(type: Int, value: Boolean) {
        when (type) {
            ExtensionFilter.DOCS -> {
                _fileStateFlow.value = _fileStateFlow.value.copy(enableRecentDocsFilter = value)
            }

            ExtensionFilter.SLIDES -> {
                _fileStateFlow.value = _fileStateFlow.value.copy(enableRecentSlidesFilter = value)
            }

            ExtensionFilter.SHEETS -> {
                _fileStateFlow.value = _fileStateFlow.value.copy(enableRecentSheetsFilter = value)
            }

            ExtensionFilter.PICTURE -> {
                _fileStateFlow.value = _fileStateFlow.value.copy(enableRecentPictureFilter = value)
            }

            ExtensionFilter.VIDEO -> {
                _fileStateFlow.value = _fileStateFlow.value.copy(enableRecentVideoFilter = value)
            }

            ExtensionFilter.AUDIO -> {
                _fileStateFlow.value = _fileStateFlow.value.copy(enableRecentAudioFilter = value)
            }

            ExtensionFilter.COMPRESSED -> {
                _fileStateFlow.value =
                    _fileStateFlow.value.copy(enableRecentCompressedFilter = value)
            }

            ExtensionFilter.PDF -> {
                _fileStateFlow.value = _fileStateFlow.value.copy(enableRecentPDFFilter = value)
            }
        }
    }

    fun setFilter(filter: TimeFilter) {
        _fileStateFlow.value = _fileStateFlow.value.copy(timeFilter = filter)
    }

    fun setFilter(filter: ExtensionFilter) {
        _fileStateFlow.value = _fileStateFlow.value.copy(extensionFilter = filter)
    }

    fun setFilter(filter: SizeFilter) {
        _fileStateFlow.value = _fileStateFlow.value.copy(sizeFilter = filter)
    }

}