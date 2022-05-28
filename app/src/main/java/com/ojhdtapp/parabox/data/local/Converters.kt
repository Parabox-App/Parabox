package com.ojhdtapp.parabox.data.local

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.message_content.At
import com.ojhdtapp.parabox.domain.model.message_content.Image
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import com.ojhdtapp.parabox.domain.model.message_content.PlainText
import com.ojhdtapp.parabox.domain.util.JsonParser

@ProvidedTypeConverter
class Converters(
    private val jsonParser: JsonParser
) {
    private val delimiter1 = "SsEePp"
    private val delimiter2 = "OoCcTt"

    @TypeConverter
    fun fromMessageContentListJson(json: String): List<MessageContent> {
        val res = mutableListOf<MessageContent>()
        json.split(delimiter1).forEach {
            val subList = it.split(delimiter2, limit = 2)
            when (subList[1].toInt()) {
                MessageContent.PLAIN_TEXT -> {
                    jsonParser.fromJson<PlainText>(
                        subList[0],
                        object : TypeToken<PlainText>() {}.type
                    )
                }
                MessageContent.IMAGE -> {
                    jsonParser.fromJson<Image>(
                        subList[0],
                        object : TypeToken<Image>() {}.type
                    )
                }
                MessageContent.AT -> {
                    jsonParser.fromJson<At>(
                        subList[0],
                        object : TypeToken<At>() {}.type
                    )
                }
                else -> null
            }?.let {
                res.add(it)
            }
        }
        return res
//        return jsonParser.fromJson<ArrayList<MessageContent>>(
//            json,
//            object : TypeToken<ArrayList<MessageContent>>() {}.type
//        ) ?: emptyList()
    }

    @TypeConverter
    fun toMessageContentListJson(messageContentList: List<MessageContent>): String {
//        return jsonParser.toJson(
//            messageContentList,
//            object : TypeToken<ArrayList<MessageContent>>() {}.type
//        ) ?: "[]"
        val str = StringBuilder()
        messageContentList.forEachIndexed { index, messageContent ->
            str.append(
                getJsonWithMessageContentTypeJudged(messageContent)
            )
            if (index != messageContentList.size - 1) {
                str.append(delimiter1)
            }
        }
        return str.toString()
    }

    private fun getJsonWithMessageContentTypeJudged(messageContent: MessageContent): String {
        val str = StringBuilder()
        str.append(
            when (messageContent) {
                is PlainText -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<PlainText>() {}.type
                    ) ?: ""
                }
                is Image -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<Image>() {}.type
                    ) ?: ""
                }
                is At -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<At>() {}.type
                    ) ?: ""
                }
                else -> ""
            }
        )
        str.append(delimiter2)
        str.append(
            when (messageContent) {
                is PlainText -> MessageContent.PLAIN_TEXT
                is Image -> MessageContent.IMAGE
                is At -> MessageContent.AT
                else -> -1
            }
        )
        return str.toString()
    }

    @TypeConverter
    fun fromMessageJson(json: String): Message {
        return jsonParser.fromJson<Message>(
            json,
            object : TypeToken<Message>() {}.type
        )!!
    }

    @TypeConverter
    fun toMessageJson(message: Message): String {
        return jsonParser.toJson(
            message.toMessageWithoutContents(),
            object : TypeToken<Message>() {}.type
        ) ?: ""
    }
}