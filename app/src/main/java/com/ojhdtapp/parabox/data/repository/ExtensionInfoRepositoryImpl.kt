package com.ojhdtapp.parabox.data.repository

import android.content.Context
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.entity.ExtensionInfoEntity
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExtensionInfoRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : ExtensionInfoRepository {
    override fun insertExtensionInfo(extensionInfo: ExtensionInfoEntity): Long {
        return db.extensionInfoDao.insertExtensionInfo(extensionInfo)
    }

    override fun deleteExtensionInfoById(extensionId: Long): Int {
        return db.extensionInfoDao.deleteExtensionInfoById(extensionId)
    }

    override fun getExtensionInfoList(): Flow<Resource<List<ExtensionInfo>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emitAll(db.extensionInfoDao.getExtensionInfoList().map {
                    Resource.Success(it.map { it.toExtensionInfo() })
                })
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }
}