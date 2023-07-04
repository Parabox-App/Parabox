package com.ojhdtapp.parabox.data.repository

import android.content.Context
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : ContactRepository {
    override fun queryContact(query: String): Flow<Resource<List<Contact>>> {
        TODO("Not yet implemented")
    }
}