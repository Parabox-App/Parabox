package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.domain.model.chat.ChatBlock
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent

data class ContactWithMessages(
    val contact: Contact,
    val messages: List<Message>
) {
    fun toTimedMessages(): Map<Long, List<ChatBlock>> {
        val resMap = mutableMapOf<Long, List<ChatBlock>>()
        val timestampMessagePairList = mutableListOf<Pair<Long, Message>>()
        this.messages.fold(timestampMessagePairList) { acc, message ->
            if (acc.isEmpty() || message.timestamp - acc.last().second.timestamp > 180000 || message.timestamp - acc.last().first > 600000) {
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
        val messageItemList = mutableListOf<List<MessageContent>>()
        var currentProfile: Profile? = null
        messagesAtSimilarTime.forEachIndexed { index, message ->
            if (currentProfile == null || message.profile == currentProfile) {
                currentProfile = message.profile
            } else {
                chatBlockList.add(ChatBlock(currentProfile!!, messageItemList))
                currentProfile = message.profile
                messageItemList.clear()
            }
            messageItemList.add(message.contents)
            if (messagesAtSimilarTime.lastIndex == index) {
                chatBlockList.add(ChatBlock(currentProfile!!, messageItemList))
            }
        }
        return chatBlockList
    }
}
