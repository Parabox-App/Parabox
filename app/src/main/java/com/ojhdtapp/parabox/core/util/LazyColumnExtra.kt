package com.ojhdtapp.parabox.core.util

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems

fun <T : Any> LazyListScope.itemsBeforeAndAfter(
    items: LazyPagingItems<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(value: T?, beforeValue: T?, afterValue: T?) -> Unit
) {
    items(
        count = items.itemCount,
        key = if (key == null) null else { index ->
            val item = items.peek(index)
            if (item == null) {
                index
            } else {
                key(item)
            }
        }
    ) { index ->
        val beforeValue = if (index - 1 < 0) null else items.peek(index - 1)
        val afterValue = if (index + 1 >= items.itemCount) null else items.peek(index + 1)
        itemContent(items[index], beforeValue, afterValue)
    }
}

fun <T : Any> LazyListScope.itemsBeforeAndAfterReverse(
    items: LazyPagingItems<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(value: T?, beforeValue: T?, afterValue: T?) -> Unit
) {
    items(
        count = items.itemCount,
        key = if (key == null) null else { index ->
            val item = items.peek(index)
            if (item == null) {
                index
            } else {
                key(item)
            }
        }
    ) { index ->
        val beforeValue = if (index - 1 < 0) null else items.peek(index - 1)
        val afterValue = if (index + 1 >= items.itemCount) null else items.peek(index + 1)
        itemContent(items[index], afterValue, beforeValue)
    }
}