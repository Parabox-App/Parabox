package com.ojhdtapp.parabox.domain.model

import android.util.Log
import androidx.paging.PagingData
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.chat.ChatBlock
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import kotlin.math.abs

data class Message(
    val contents: List<MessageContent>,
    val profile: Profile,
    val timestamp: Long,
    val messageId: Long
) {
    fun toMessageEntity(id: Int): MessageEntity {
        return MessageEntity(
            contents = contents,
            profile = profile,
            timestamp = timestamp,
            messageId = messageId
        )
    }

    fun toMessageWithoutContents(): Message {
        return Message(
            contents = emptyList(),
            profile = profile,
            timestamp = timestamp,
            messageId = messageId
        )
    }
}

fun List<Message>.toTimedMessages(): Map<Long, List<ChatBlock>>{
    Log.d("parabox", "Huge work for timing")
    val resMap = mutableMapOf<Long, List<ChatBlock>>()
    val timestampMessagePairList = mutableListOf<Pair<Long, Message>>()
    this.fold(timestampMessagePairList) { acc, message ->
        if (acc.isEmpty() || abs(message.timestamp - acc.last().second.timestamp) > 180000 || abs(message.timestamp - acc.last().first) > 600000) {
            acc.add(message.timestamp to message)
        } else {
            acc.add((acc.last().first to message))
        }
        acc
    }.groupBy(
        keySelector = { it.first },
        valueTransform = { value ->
            value.second
        }
    ).forEach { (timestamp, messagesAtSimilarTime) ->
        resMap[timestamp] = messagesToChatBlocks(messagesAtSimilarTime)
    }
    return resMap
}

private fun messagesToChatBlocks(messagesAtSimilarTime:List<Message>):List<ChatBlock>{
    val chatBlockList = mutableListOf<ChatBlock>()
    val messageItemList = mutableListOf<Message>()
    var currentProfile: Profile? = null
    messagesAtSimilarTime.forEachIndexed { index, message ->
        if (currentProfile == null || message.profile == currentProfile) {
            currentProfile = message.profile
        } else {
            // reverse here
            chatBlockList.add(ChatBlock(currentProfile!!, messageItemList.toList().reversed()))
            currentProfile = message.profile
            messageItemList.clear()
        }
        messageItemList.add(message)
        if (messagesAtSimilarTime.lastIndex == index) {
            chatBlockList.add(ChatBlock(currentProfile!!, messageItemList.toList().reversed()))
        }
    }
    return chatBlockList
}

//fun List<Message>.toTimedMessagesReverse(): Map<Long, List<ChatBlock>>{
//    val resMap = mutableMapOf<Long, List<ChatBlock>>()
//    val timestampMessagePairList = mutableListOf<Pair<Long, Message>>()
//}