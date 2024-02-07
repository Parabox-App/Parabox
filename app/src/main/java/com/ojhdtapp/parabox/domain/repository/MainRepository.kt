package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun receiveMessage(msg: ReceiveMessage, ext: Extension.ExtensionSuccess): ParaboxResult

    fun getRecentQuery(): Flow<Resource<List<RecentQuery>>>

    suspend fun submitRecentQuery(value: String): Boolean

    suspend fun deleteRecentQuery(id: Long): Boolean
}