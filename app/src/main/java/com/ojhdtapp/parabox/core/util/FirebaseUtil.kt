package com.ojhdtapp.parabox.core.util

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.ojhdtapp.parabox.domain.model.ChangeLog
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FirebaseUtil {
    const val FIRE_STORE_CONFIG = "config"
    const val FIRE_STORE_CHANGELOG = "change_log"

    val firestore by lazy {
        Firebase.firestore
    }

    suspend fun getChangeLogFromFireStore() : List<ChangeLog> {
        return suspendCoroutine<List<ChangeLog>> { cot ->
            firestore.collection(FIRE_STORE_CHANGELOG)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val res = it.result.documents.map {
                            it.toObject(ChangeLog::class.java)
                        }.filterNotNull()
                        cot.resume(res)
                    } else {
                        cot.resumeWithException(it.exception?.cause ?: Exception("Error getting documents"))
                    }
                }
        }
    }
}