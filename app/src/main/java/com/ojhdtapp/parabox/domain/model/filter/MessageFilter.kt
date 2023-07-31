package com.ojhdtapp.parabox.domain.model.filter

import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.model.Message
import kotlin.math.abs

sealed class MessageFilter(
    open val labelResId: Int,
    open val check: (message: Message) -> Boolean
) {

    sealed class SenderFilter(
        override val labelResId: Int,
        override val check: (message: Message) -> Boolean
    ) : MessageFilter(labelResId, check) {
        object All : SenderFilter(
            R.string.message_filter_sender_all,
            check = { true }
        )

        data class Custom(
            val senderName: String,
            val senderId: Long
        ) : SenderFilter(
            labelResId = -1,
            check = { message: Message ->
                message.senderId == senderId
            }
        ) {
            val label = "来自${senderName}"
        }
    }

    sealed class ChatFilter(
        override val labelResId: Int,
        override val check: (message: Message) -> Boolean
    ) : MessageFilter(labelResId, check) {
        object All : ChatFilter(
            R.string.message_filter_chat_all,
            check = { true }
        )

        data class Custom(
            val chatName: String,
            val chatId: Long
        ) : ChatFilter(
            labelResId = -1,
            check = { message: Message ->
                message.chatId == chatId
            }
        ) {
            val label = "属于${chatName}"
        }
    }

    sealed class TimeFilter(
        override val labelResId: Int,
        override val check: (message: Message) -> Boolean
    ) : MessageFilter(labelResId, check) {
        object All : TimeFilter(R.string.time_filter_all_label, { true })
        object WithinThreeDays : TimeFilter(
            R.string.time_filter_within_three_days_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) < 259200000 })

        object WithinThisWeek : TimeFilter(
            R.string.time_filter_within_this_week_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) < 604800000 }
        )

        object WithinThisMonth : TimeFilter(
            R.string.time_filter_within_this_month_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) < 2592000000 }
        )

        object MoreThanAMonth : TimeFilter(
            R.string.time_filter_more_than_a_month_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) >= 2592000000 }
        )

        data class Custom(
            val timestampStart: Long? = null,
            val timestampEnd: Long? = null
        ) :
            TimeFilter(
//            label = "从 ${timestampStart?.toFormattedDate() ?: "不受限制"} 到 ${timestampEnd?.toFormattedDate() ?: "不受限制"}",
                R.string.time_filter_custom_label,
                check = { message: Message ->
                    message.timestamp in (timestampStart ?: System.currentTimeMillis()) until (timestampEnd
                        ?: System.currentTimeMillis())
                }
            )
    }
}
