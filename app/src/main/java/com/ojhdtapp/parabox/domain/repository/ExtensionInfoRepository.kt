package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.entity.ExtensionInfoEntity
import kotlinx.coroutines.flow.Flow

interface ExtensionInfoRepository {
    fun insertExtensionInfo(extensionInfo: ExtensionInfoEntity): Long
    fun deleteExtensionInfoById(extensionId: Long): Int
    fun getExtensionInfoList() : Flow<Resource<List<ExtensionInfo>>>
}