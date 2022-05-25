package com.ojhdtapp.parabox.core.util

import kotlinx.coroutines.CompletableDeferred

class CompletableDeferredWithTag<U, V>(
    private val savedTag: U,
    private val deferred: CompletableDeferred<V> = CompletableDeferred()
) {
    fun cancel() {
        deferred.cancel()
    }
    suspend fun await() = deferred.await()
    fun complete(tag: U, value: V): Boolean =
        if (tag == savedTag)
            deferred.complete(value)
        else false
    fun getCurrentTag(): U = savedTag
}