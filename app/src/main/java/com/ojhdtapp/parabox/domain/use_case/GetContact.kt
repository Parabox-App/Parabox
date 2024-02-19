package com.ojhdtapp.parabox.domain.use_case

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetContact @Inject constructor(
    val repository: ContactRepository
) {
    fun byId(contactId: Long): Flow<Resource<Contact>> {
        return repository.getContactById(contactId)
    }

    fun byPlatformInfo(pkg: String, uid: String): Flow<Resource<Contact>> {
        return repository.getContactByPlatformInfo(pkg, uid)
    }

    fun pagingSource(): Flow<PagingData<ContactWithExtensionInfo>>{
        return Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                initialLoadSize = 20
            )
        ){ repository.getContactWithExtensionInfoPagingSource() }
            .flow
            .map { pagingData ->
                pagingData.map {
                    it.toContactWithExtensionInfo()
                }
            }
    }
}