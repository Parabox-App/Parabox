package com.ojhdt.parabox.extension.demo.util

import cn.evole.onebot.sdk.entity.ArrayMsg
import cn.evole.onebot.sdk.enums.MsgTypeEnum
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

fun ArrayMsg.toParaboxMessageElement(): ParaboxMessageElement? {
    return try {
        when (type) {
            MsgTypeEnum.at -> ParaboxAt(
                target = ParaboxContact(
                    name = null,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
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
                    name = null,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                    uid = ""
                ), messageUUID = data.get("id")!!
            )

            else -> ParaboxPlainText("不支持的类型")
        }
    } catch (e: Exception) {
        null
    }
}