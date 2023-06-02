package com.ojhdtapp.paraboxdevelopmentkit.model.res_info

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

sealed interface ParaboxResourceInfo : Parcelable {
    @Parcelize
    @Serializable
    object ParaboxEmptyInfo: ParaboxResourceInfo
    sealed interface ParaboxLocalInfo : ParaboxResourceInfo {
        suspend fun upload(service: ParaboxCloudService): ParaboxSyncedInfo {
            return ParaboxSyncedInfo(
                local = this,
                remote = service.upload()
            )
        }

        @Parcelize
        @Serializable
        data class UriLocalInfo(@Contextual val uri: Uri) : ParaboxLocalInfo
    }

    sealed interface ParaboxRemoteInfo : ParaboxResourceInfo {
        suspend fun download(service: ParaboxCloudService): ParaboxSyncedInfo {
            return ParaboxSyncedInfo(
                local = service.download(),
                remote = this
            )
        }
        @Parcelize
        @Serializable
        data class UrlRemoteInfo(val url: String) : ParaboxRemoteInfo
        @Parcelize
        @Serializable
        data class DriveRemoteInfo(val uuid: String, val cloudPath: String) : ParaboxRemoteInfo
    }

    @Parcelize
    @Serializable
    class ParaboxSyncedInfo(val local: ParaboxLocalInfo, val remote: ParaboxRemoteInfo) :
        ParaboxLocalInfo, ParaboxRemoteInfo
}

object URISerializer : KSerializer<Uri>{
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parse(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Uri) {
        return encoder.encodeString(value.toString())
    }

}