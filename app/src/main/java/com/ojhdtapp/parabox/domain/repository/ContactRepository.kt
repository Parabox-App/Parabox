package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactWithExtensionInfoEntity
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun queryContactWithLimit(query: String, limit: Int): Flow<Resource<List<Contact>>>
    fun getContactWithLimit(limit: Int): Flow<Resource<List<Contact>>>
    fun getContactById(contactId: Long): Flow<Resource<Contact>>
    fun getContactByPlatformInfo(pkg: String, uid: String): Flow<Resource<Contact>>
    fun getContactPagingSource() : PagingSource<Int, ContactEntity>
    fun getContactWithExtensionInfoPagingSource() : PagingSource<Int, ContactWithExtensionInfoEntity>
}