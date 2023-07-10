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

    fun getModel(): Any?

    operator fun plus(otherResourceInfo: ParaboxResourceInfo): ParaboxResourceInfo?

    @Parcelize
    @Serializable
    object ParaboxEmptyInfo: ParaboxResourceInfo {
        override fun getModel(): Any? {
            return null
        }

        override operator fun plus(otherResourceInfo: ParaboxResourceInfo): ParaboxResourceInfo? {
            return otherResourceInfo
        }
    }

    sealed interface ParaboxLocalInfo : ParaboxResourceInfo {
        suspend fun upload(service: ParaboxCloudService): ParaboxSyncedInfo?

        override operator fun plus(otherResourceInfo: ParaboxResourceInfo): ParaboxResourceInfo? {
            return if(otherResourceInfo is ParaboxRemoteInfo){
                ParaboxSyncedInfo(local = this, remote = otherResourceInfo)
            } else null
        }
        @Parcelize
        @Serializable
        data class UriLocalInfo(@Contextual val uri: Uri) : ParaboxLocalInfo {
            override suspend fun upload(service: ParaboxCloudService): ParaboxSyncedInfo? {
                return (service.upload(uri) + this) as? ParaboxSyncedInfo
            }

            override fun getModel(): Any? {
                return uri
            }
        }
    }

    sealed interface ParaboxRemoteInfo : ParaboxResourceInfo {
        suspend fun download(service: ParaboxCloudService): ParaboxSyncedInfo?

        override operator fun plus(otherResourceInfo: ParaboxResourceInfo): ParaboxResourceInfo? {
            return if(otherResourceInfo is ParaboxLocalInfo){
                ParaboxSyncedInfo(local = otherResourceInfo, remote = this)
            } else null
        }
        @Parcelize
        @Serializable
        data class UrlRemoteInfo(val url: String) : ParaboxRemoteInfo {
            override suspend fun download(service: ParaboxCloudService): ParaboxSyncedInfo? {
                return (service.download(url) + this) as? ParaboxSyncedInfo
            }

            override fun getModel(): Any? {
                return url
            }
        }

        @Parcelize
        @Serializable
        data class DriveRemoteInfo(val uuid: String, val cloudPath: String, val driveType: Int) : ParaboxRemoteInfo {
            override suspend fun download(service: ParaboxCloudService): ParaboxSyncedInfo? {
                return (service.download(uuid, cloudPath, driveType) + this) as? ParaboxSyncedInfo
            }

            override fun getModel(): Any? {
                return null
            }
        }
    }

    @Parcelize
    @Serializable
    class ParaboxSyncedInfo(val local: ParaboxLocalInfo, val remote: ParaboxRemoteInfo) :
        ParaboxResourceInfo {
        override fun getModel(): Any? {
            return local.getModel() ?: remote.getModel()
        }

        override fun plus(otherResourceInfo: ParaboxResourceInfo): ParaboxResourceInfo? {
            return when(otherResourceInfo){
                is ParaboxLocalInfo -> {
                    ParaboxSyncedInfo(otherResourceInfo, remote)
                }

                is ParaboxRemoteInfo -> {
                    ParaboxSyncedInfo(local, otherResourceInfo)
                }

                is ParaboxSyncedInfo -> {
                    otherResourceInfo
                }

                else -> this
            }
        }
    }
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