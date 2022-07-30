package com.ojhdtapp.parabox.data.local.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ojhdtapp.parabox.data.local.entity.ContactEntity

class MessageEntityPagingSource : PagingSource<Int, ContactEntity>() {
    override fun getRefreshKey(state: PagingState<Int, ContactEntity>): Int? {
        TODO("Not yet implemented")
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContactEntity> {
        TODO("Not yet implemented")
    }
}