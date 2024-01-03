package com.ojhdt.parabox.extension.demo.util

import cn.evole.onebot.sdk.entity.ArrayMsg
import cn.evole.onebot.sdk.enums.MsgTypeEnum
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

fun ArrayMsg.toParaboxMessageElement(): ParaboxMessageElement? {
    return try {
        when (type) {
            MsgTypeEnum.at -> ParaboxAt(
                target = ParaboxContact(
                    basicInfo = ParaboxBasicInfo(
                        name = null,
                        avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                    ),
                    uid = data["qq"]!!
                )
            )

            MsgTypeEnum.text -> ParaboxPlainText(text = data["text"]!!)
            MsgTypeEnum.image -> ParaboxImage(
                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(
                    url = data["url"]!!
                )
            )

            MsgTypeEnum.reply -> ParaboxQuoteReply(
                belong = ParaboxContact(
                    basicInfo = ParaboxBasicInfo(
                        name = null,
                        avatar = ParaboxResourceInfo.ParaboxEmptyInfo
                    ),
                    uid = ""
                ), messageUUID = data["id"]!!
            )

            MsgTypeEnum.face -> ParaboxPlainText(text = (data["id"]?.toInt())?.queryFace() ?: "[表情]")

            else -> ParaboxUnsupported
        }
    } catch (e: Exception) {
        null
    }
}

fun Int.queryFace(): String? {
    return when (this) {
        0 -> "😲"
        1 -> "😖"
        2 -> "😍"
        3 -> "😶"
        4 -> "😎"
        5 -> "😭"
        6 -> "😳"
        7 -> "🤐"
        8 -> "😴"
        9 -> "😢"
        10 -> "😟"
        11 -> "😡"
        12 -> "🤪"
        13 -> "😁"
        14 -> "😊"
        15 -> "☹️"
        16 -> "😎"
        96 -> "😓"
        18 -> "😫"
        19 -> "🤮"
        20 -> "🤭"
        21 -> "😊"
        22 -> "🙄"
        23 -> "😤"
        24 -> "🥴"
        25 -> "😪"
        26 -> "😲"
        27 -> "😓"
        28 -> "😄"
        29 -> "😙"
        30 -> "✊"
        31 -> "🤬"
        32 -> "😕"
        33 -> "🤫"
        34 -> "😵‍💫"
        35 -> "😣"
        36 -> "🤯"
        37 -> "💀"
        38 -> "😡"
        39 -> "👋"
        40 -> "😑"
        97 -> "😅"
        98 -> "😪"
        99 -> "👏"
        100 -> "😓"
        101 -> "😁"
        102 -> "😤"
        103 -> "😤"
        104 -> "🥱"
        105 -> "😒"
        106 -> "😟"
        107 -> "😞"
        108 -> "🥸"
        109 -> "😙"
        110 -> "😲"
        111 -> "🥺"
        172 -> "😜"
        182 -> "😂"
        179 -> "😊"
        173 -> "😭"
        174 -> "😛"
        212 -> "😶"
        175 -> "😛"
        178 -> "😆"
        177 -> "🤢"
        180 -> "😃"
        181 -> "😐"
        176 -> "😊"
        183 -> "😝"
        293 -> "🐟"
        else -> null
    }
}