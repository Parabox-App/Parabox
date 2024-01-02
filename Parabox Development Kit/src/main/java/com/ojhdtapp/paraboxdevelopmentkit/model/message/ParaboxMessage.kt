package com.ojhdtapp.paraboxdevelopmentkit.model.message

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ParaboxAnnotatedText(val list: List<ParaboxText>) : ParaboxMessageElement {
    override fun contentToString(): String {
        return list.joinToString(" ")
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.ANNOTATED_TEXT.ordinal
    }

}

sealed interface ParaboxText: ParaboxMessageElement

@Parcelize
@Serializable
data class ParaboxAt(val target: ParaboxContact) : ParaboxText {
    override fun contentToString(): String {
        return "@${target.basicInfo.name ?: target.uid}"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.AT.ordinal
    }
}

@Parcelize
@Serializable
object ParaboxAtAll : ParaboxText {
    override fun contentToString(): String {
        return "@全体成员"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.AT_ALL.ordinal }
}

@Parcelize
@Serializable
data class ParaboxAudio(
    val length: Long = 0L,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val resourceInfo: ParaboxResourceInfo
) : ParaboxMessageElement {
    override fun contentToString(): String {
        return "[语音消息]"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.AUDIO.ordinal
    }
}

@Parcelize
@Serializable
data class ParaboxFile(
    val name: String,
    val extension: String,
    val size: Long,
    val lastModifiedTime: Long,
    val resourceInfo: ParaboxResourceInfo
) : ParaboxMessageElement {
    override fun contentToString(): String {
        return "[文件]${name}"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.FILE.ordinal
    }
}

@Parcelize
@Serializable
data class ParaboxImage(
    val width: Int = 0,
    val height: Int = 0,
    val fileName: String? = null,
    val resourceInfo: ParaboxResourceInfo
) : ParaboxMessageElement {
    override fun contentToString(): String {
        return "[图片]"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.IMAGE.ordinal
    }
}

@Parcelize
@Serializable
data class ParaboxLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val description: String?
) : ParaboxMessageElement {
    override fun contentToString(): String {
        return "[位置]"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.LOCATION.ordinal
    }
}

@Parcelize
@Serializable
data class ParaboxPlainText(
    val text: String
) : ParaboxText {
    override fun contentToString(): String {
        return text
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.PLAIN_TEXT.ordinal
    }
}

@Serializable
@Parcelize
data class ParaboxQuoteReply(
    val belong: ParaboxContact,
    val messageUUID: String
) : ParaboxMessageElement {
    override fun contentToString(): String {
        return "[引用回复]"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.QUOTE_REPLY.ordinal
    }
}

@Parcelize
@Serializable
data class ParaboxForward(
    val list: List<ForwardNode>
): ParaboxMessageElement {
    @Parcelize
    @Serializable
    data class ForwardNode(
        val sender: ParaboxContact,
        val timestamp: Long,
        val messages: List<ParaboxMessageElement>
    ) : Parcelable

    override fun contentToString(): String {
        return "[合并转发]"
    }

    override fun getType(): Int {
        return ParaboxMessageElement.Companion.TYPE.FORWARD.ordinal
    }
}