package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.GroupInfoPack
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetGroupInfoPack @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(contactIds: List<Long>): Flow<Resource<GroupInfoPack>> {
        return flow {
            emit(Resource.Loading())
            withContext(Dispatchers.IO) {
                repository.getGroupInfoPack(contactIds = contactIds)
            }.also {
                if (it == null) emit(Resource.Error("加载数据时发生错误"))
                else emit(Resource.Success(it))
            }
        }
    }
}