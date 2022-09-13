package com.ojhdtapp.parabox.data.local

import android.util.Log
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.*
import com.ojhdtapp.parabox.domain.util.JsonParser

@ProvidedTypeConverter
class Converters(
    private val jsonParser: JsonParser
) {
    private val delimiter1 = "SsEePp"
    private val delimiter2 = "OoCcTt"
    private val delimiter3 = "NnOoVv"
    private val delimiter4 = "DdEeCc"
    private val delimiter5 = "JjAaNn"

    @TypeConverter
    fun fromMessageContentListJson(json: String): List<MessageContent> {
        val res = mutableListOf<MessageContent>()
        json.split(delimiter1).forEach {
            val subList = it.split(delimiter2, limit = 2)
            if (subList.size == 2) {
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
                    MessageContent.AT_ALL -> {
                        jsonParser.fromJson<AtAll>(
                            subList[0],
                            object : TypeToken<AtAll>() {}.type
                        )
                    }
                    MessageContent.AUDIO -> {
                        jsonParser.fromJson<Audio>(
                            subList[0],
                            object : TypeToken<Audio>() {}.type
                        )
                    }
                    MessageContent.QUOTE_REPLY -> {
                        val quoteReplySubList = subList[0].split(delimiter3, limit = 2)
                        val metadata = jsonParser.fromJson<QuoteReplyMetadata>(
                            quoteReplySubList[0],
                            object : TypeToken<QuoteReplyMetadata>() {}.type
                        )
                        val content = fromQuoteMessageContentListJson(quoteReplySubList[1])
                        QuoteReply(
                            metadata?.quoteMessageSenderName,
                            metadata?.quoteMessageTimestamp,
                            metadata?.quoteMessageId,
                            content
                        )
                    }
                    MessageContent.FILE -> {
                        jsonParser.fromJson<File>(
                            subList[0],
                            object : TypeToken<File>() {}.type
                        )
                    }
                    else -> null
                }?.let {
                    res.add(it)
                }
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

    @TypeConverter
    fun fromPluginConnectionListJson(json: String): List<PluginConnection> {
        return jsonParser.fromJson<ArrayList<PluginConnection>>(
            json,
            object : TypeToken<ArrayList<PluginConnection>>() {}.type
        ) ?: emptyList()
    }

    @TypeConverter
    fun toPluginConnectionListJson(pluginConnectionList: List<PluginConnection>): String {
        return jsonParser.toJson(
            pluginConnectionList,
            object : TypeToken<ArrayList<PluginConnection>>() {}.type
        ) ?: "[]"
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
                is AtAll -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<AtAll>() {}.type
                    ) ?: ""
                }
                is Audio -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<Audio>() {}.type
                    ) ?: ""
                }
                is QuoteReply -> {
                    val metadata = QuoteReplyMetadata(
                        messageContent.quoteMessageSenderName,
                        messageContent.quoteMessageTimestamp,
                        messageContent.quoteMessageId
                    )
                    val contentJson = messageContent.quoteMessageContent?.let {
                        toQuoteMessageContentListJson(it)
                    } ?: ""
                    val metadataJson = jsonParser.toJson(
                        metadata,
                        object : TypeToken<QuoteReplyMetadata>() {}.type
                    ) ?: ""
                    "$metadataJson$delimiter3$contentJson"
                }
                is File -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<File>() {}.type
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
                is AtAll -> MessageContent.AT_ALL
                is Audio -> MessageContent.AUDIO
                is QuoteReply -> MessageContent.QUOTE_REPLY
                is File -> MessageContent.FILE
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

    @TypeConverter
    fun fromTagsJson(json: String): List<String> {
        return jsonParser.fromJson<List<String>>(
            json,
            object : TypeToken<List<String>>() {}.type
        )!!
    }

    @TypeConverter
    fun toTagsJson(tags: List<String>): String {
        return jsonParser.toJson(
            tags,
            object : TypeToken<List<String>>() {}.type
        ) ?: ""
    }

    private fun fromQuoteMessageContentListJson(json: String): List<MessageContent> {
        val res = mutableListOf<MessageContent>()
        json.split(delimiter4).forEach {
            val subList = it.split(delimiter5, limit = 2)
            if (subList.size == 2) {
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
                    MessageContent.AT_ALL -> {
                        jsonParser.fromJson<AtAll>(
                            subList[0],
                            object : TypeToken<AtAll>() {}.type
                        )
                    }
                    MessageContent.AUDIO -> {
                        jsonParser.fromJson<Audio>(
                            subList[0],
                            object : TypeToken<Audio>() {}.type
                        )
                    }
                    MessageContent.QUOTE_REPLY -> null
                    MessageContent.FILE -> {
                        jsonParser.fromJson<File>(
                            subList[0],
                            object : TypeToken<File>() {}.type
                        )
                    }
                    else -> null
                }?.let {
                    res.add(it)
                }
            }
        }
        return res
//        return jsonParser.fromJson<ArrayList<MessageContent>>(
//            json,
//            object : TypeToken<ArrayList<MessageContent>>() {}.type
//        ) ?: emptyList()
    }

    private fun toQuoteMessageContentListJson(messageContentList: List<MessageContent>): String {
//        return jsonParser.toJson(
//            messageContentList,
//            object : TypeToken<ArrayList<MessageContent>>() {}.type
//        ) ?: "[]"
        val str = StringBuilder()
        messageContentList.forEachIndexed { index, messageContent ->
            str.append(
                getQuoteJsonWithMessageContentTypeJudged(messageContent)
            )
            if (index != messageContentList.size - 1) {
                str.append(delimiter4)
            }
        }
        return str.toString()
    }

    private fun getQuoteJsonWithMessageContentTypeJudged(messageContent: MessageContent): String {
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
                is AtAll -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<AtAll>() {}.type
                    ) ?: ""
                }
                is Audio -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<Audio>() {}.type
                    ) ?: ""
                }
                is QuoteReply -> ""
                is File -> {
                    jsonParser.toJson(
                        messageContent,
                        object : TypeToken<File>() {}.type
                    ) ?: ""
                }
                else -> ""
            }
        )
        str.append(delimiter5)
        str.append(
            when (messageContent) {
                is PlainText -> MessageContent.PLAIN_TEXT
                is Image -> MessageContent.IMAGE
                is At -> MessageContent.AT
                is AtAll -> MessageContent.AT_ALL
                is Audio -> MessageContent.AUDIO
                is QuoteReply -> MessageContent.QUOTE_REPLY
                is File -> MessageContent.FILE
                else -> -1
            }
        )
        return str.toString()
    }

    @TypeConverter
    fun fromDownloadingStateJson(json: String): DownloadingState {
        val list = json.split(delimiter1, limit = 2)
        return when(list[1]){
            "downloading" -> jsonParser.fromJson<DownloadingState.Downloading>(
                list[0],
                object : TypeToken<DownloadingState.Downloading>() {}.type
            )!!
            "none" -> jsonParser.fromJson<DownloadingState.None>(
                list[0],
                object : TypeToken<DownloadingState.None>() {}.type
            )!!
            "failure" -> jsonParser.fromJson<DownloadingState.Failure>(
                list[0],
                object : TypeToken<DownloadingState.Failure>() {}.type
            )!!
            "done" -> jsonParser.fromJson<DownloadingState.Done>(
                list[0],
                object : TypeToken<DownloadingState.Done>() {}.type
            )!!
            else -> throw Exception("error converting")
        }
    }

    @TypeConverter
    fun toDownloadingStateJson(state: DownloadingState): String {
        val stateJson = when(state){
            is DownloadingState.Downloading ->jsonParser.toJson(
                state,
                object : TypeToken<DownloadingState.Downloading>() {}.type
            ) ?: ""
            is DownloadingState.None ->jsonParser.toJson(
                state,
                object : TypeToken<DownloadingState.None>() {}.type
            ) ?: ""
            is DownloadingState.Failure ->jsonParser.toJson(
                state,
                object : TypeToken<DownloadingState.Failure>() {}.type
            ) ?: ""
            is DownloadingState.Done ->jsonParser.toJson(
                state,
                object : TypeToken<DownloadingState.Done>() {}.type
            ) ?: ""
        }
        val stateNum = when(state){
            is DownloadingState.Downloading -> "downloading"
            is DownloadingState.None -> "none"
            is DownloadingState.Failure -> "failure"
            is DownloadingState.Done -> "done"
        }
        return "${stateJson}${delimiter1}${stateNum}"
    }
}