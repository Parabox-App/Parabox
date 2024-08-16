package com.ojhdtapp.parabox.domain.repository

import android.os.Bundle
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

interface ConnectionInfoRepository {
    fun insertConnectionInfo(connectionInfo: ConnectionInfoEntity): Long
    fun deleteConnectionInfoById(connectionId: Long): Int
    fun deleteConnectionInfoByPackageName(pkg: String): Int
    fun getConnectionInfoList() : Flow<Resource<List<ConnectionInfo>>>
    fun updateConnectionInfoExtra(connectionId: Long, extra: JSONObject): Boolean
}