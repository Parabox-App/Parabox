package com.ojhdtapp.parabox.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : ContactRepository {
    override fun queryContactWithLimit(query: String, limit: Int): Flow<Resource<List<Contact>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    if (limit == 0) {
                        Resource.Success(db.contactDao.queryContact(query).map { it.toContact() })
                    } else {
                        Resource.Success(db.contactDao.queryContactWithLimit(query, limit).map { it.toContact() })
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun getContactWithLimit(limit: Int): Flow<Resource<List<Contact>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    withContext(Dispatchers.IO) {
                        Resource.Success(db.contactDao.getContactWithLimit(limit).map { it.toContact() })
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun getContactById(contactId: Long): Flow<Resource<Contact>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    withContext(Dispatchers.IO) {
                        db.contactDao.getContactById(contactId)?.toContact()?.let {
                            Resource.Success(it)
                        }
                    } ?: Resource.Error("not found")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun getContactByPlatformInfo(pkg: String, uid: String): Flow<Resource<Contact>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    withContext(Dispatchers.IO) {
                        db.contactDao.getContactByPlatformInfo(pkg, uid)?.toContact()?.let {
                            Resource.Success(it)
                        }
                    } ?: Resource.Error("not found")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun getContactPagingSource(): PagingSource<Int, ContactEntity> {
        return db.contactDao.getContactPagingSource()
    }
}