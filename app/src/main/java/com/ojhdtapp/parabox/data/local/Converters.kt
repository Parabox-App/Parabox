package com.ojhdtapp.parabox.data.local

import android.os.Bundle
import android.os.Parcel
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.ojhdtapp.parabox.domain.util.JsonParser
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAtAll
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf

@ProvidedTypeConverter
class Converters(
    private val jsonParser: JsonParser
) {

    @TypeConverter
    fun fromBundle(bundle: Bundle): ByteArray {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(bundle)
            return parcel.marshall()
        } finally {
            parcel.recycle()
        }
    }
    
    @TypeConverter
    fun toBundle(data: ByteArray): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(data, 0, data.size)
            parcel.setDataPosition(0)
            return requireNotNull(parcel.readBundle())
        } finally {
            parcel.recycle()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @TypeConverter
    fun fromEncodedMessage(value: String): List<ParaboxMessageElement> {
        return messageProtobuf.decodeFromHexString(value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @TypeConverter
    fun encodeToMessage(value: List<ParaboxMessageElement>): String {
        return messageProtobuf.encodeToHexString(value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @TypeConverter
    fun fromEncodedResource(value: String): ParaboxResourceInfo {
        return resourceProtobuf.decodeFromHexString(value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @TypeConverter
    fun encodeToResource(value: ParaboxResourceInfo): String {
        return resourceProtobuf.encodeToHexString(value)
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

    @TypeConverter
    fun fromChatIdsJson(json: String): List<Long> {
        return jsonParser.fromJson<List<Long>>(
            json,
            object : TypeToken<List<Long>>() {}.type
        )!!
    }

    @TypeConverter
    fun toChatIdsJson(chatIds: List<Long>): String {
        return jsonParser.toJson(
            chatIds,
            object : TypeToken<List<Long>>() {}.type
        ) ?: ""
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val messageProtobuf = ProtoBuf {
            serializersModule = SerializersModule {
                polymorphic(ParaboxMessageElement::class) {
                    subclass(ParaboxAt::class, ParaboxAt.serializer())
                    subclass(ParaboxAtAll::class, ParaboxAtAll.serializer())
                    subclass(ParaboxAudio::class, ParaboxAudio.serializer())
                    subclass(ParaboxFile::class, ParaboxFile.serializer())
                    subclass(ParaboxImage::class, ParaboxImage.serializer())
                    subclass(ParaboxLocation::class, ParaboxLocation.serializer())
                    subclass(ParaboxPlainText::class, ParaboxPlainText.serializer())
                    subclass(ParaboxQuoteReply::class, ParaboxQuoteReply.serializer())
                    subclass(ParaboxForward::class, ParaboxForward.serializer())
                    subclass(ParaboxUnsupported::class, ParaboxUnsupported.serializer())
                }
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        private val resourceProtobuf = ProtoBuf {
            serializersModule = SerializersModule {
                polymorphic(ParaboxResourceInfo::class) {
                    subclass(ParaboxResourceInfo.ParaboxEmptyInfo::class, ParaboxResourceInfo.ParaboxEmptyInfo.serializer())
                    subclass(ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo::class, ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo.serializer())
                    subclass(ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo::class, ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo.serializer())
                    subclass(ParaboxResourceInfo.ParaboxRemoteInfo.DriveRemoteInfo::class, ParaboxResourceInfo.ParaboxRemoteInfo.DriveRemoteInfo.serializer())
                    subclass(ParaboxResourceInfo.ParaboxSyncedInfo::class, ParaboxResourceInfo.ParaboxSyncedInfo.serializer())
                }
            }
        }
    }
}