package com.ojhdtapp.parabox.data.local

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.reflect.TypeToken
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import com.ojhdtapp.parabox.domain.util.JsonParser

@ProvidedTypeConverter
class Converters(
    private val jsonParser: JsonParser
) {
    @TypeConverter
    fun fromMessageContentListJson(json: String): List<MessageContent> {
        return jsonParser.fromJson<ArrayList<MessageContent>>(json,
            object : TypeToken<ArrayList<MessageContent>>() {}.type
        )?: emptyList()
    }

    @TypeConverter
    fun toMessageContentListJson(messageContentList: List<MessageContent>): String {
        return jsonParser.toJson(
            messageContentList,
            object : TypeToken<ArrayList<MessageContent>>() {}.type
        ) ?: "[]"
    }
}