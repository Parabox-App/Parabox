package com.ojhdtapp.parabox.data.repository

import android.content.Context
import android.os.Bundle
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoEntity
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoExtraUpdate
import com.ojhdtapp.parabox.domain.repository.ConnectionInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConnectionInfoRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : ConnectionInfoRepository {
    override fun insertConnectionInfo(connectionInfo: ConnectionInfoEntity): Long {
        return db.connectionInfoDao.insertConnectionInfo(connectionInfo)
    }

    override fun deleteConnectionInfoById(connectionId: Long): Int {
        return db.connectionInfoDao.deleteConnectionInfoById(connectionId)
    }

    override fun deleteConnectionInfoByPackageName(pkg: String): Int {
        return db.connectionInfoDao.deleteConnectionInfoByPackageName(pkg)
    }

    override fun getConnectionInfoList(): Flow<Resource<List<ConnectionInfo>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emitAll(db.connectionInfoDao.getConnectionInfoList().map {
                    Resource.Success(it.map { it.toConnectionInfo() })
                })
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun updateConnectionInfoExtra(connectionId: Long, extra: Bundle): Boolean {
        return db.connectionInfoDao.updateConnectionInfoExtra(ConnectionInfoExtraUpdate(connectionId, extra)) == 1
    }
}