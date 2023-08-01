package com.ojhdtapp.parabox.domain.model.filter

import android.content.Context
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.toFormattedDate
import com.ojhdtapp.parabox.domain.model.Message
import kotlin.math.abs

sealed class MessageFilter(
    open val label: String? = null,
    open val labelResId: Int,
    open val check: (message: Message) -> Boolean
) {
    open fun getLabel(context: Context): String? = label

    sealed class SenderFilter(
        override val labelResId: Int,
        override val check: (message: Message) -> Boolean
    ) : MessageFilter(labelResId = labelResId, check = check) {
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
            override val label = "来自${senderName}"
        }
    }

    sealed class ChatFilter(
        override val labelResId: Int,
        override val check: (message: Message) -> Boolean
    ) : MessageFilter(labelResId = labelResId, check = check) {
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
            override val label = "属于${chatName}"
        }
    }

    sealed class DateFilter(
        override val labelResId: Int,
        override val check: (message: Message) -> Boolean,
        override val label: String? = null,
    ) : MessageFilter(labelResId = labelResId, check = check) {
        object All : DateFilter(R.string.time_filter_all_label, { message: Message -> true })
        object WithinThreeDays : DateFilter(
            R.string.time_filter_within_three_days_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) < 259200000 })

        object WithinThisWeek : DateFilter(
            R.string.time_filter_within_this_week_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) < 604800000 }
        )

        object WithinThisMonth : DateFilter(
            R.string.time_filter_within_this_month_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) < 2592000000 }
        )

        object MoreThanAMonth : DateFilter(
            R.string.time_filter_more_than_a_month_label,
            { message: Message -> abs(System.currentTimeMillis() - message.timestamp) >= 2592000000 }
        )

        data class Custom(
            val timestampStart: Long? = null,
            val timestampEnd: Long? = null
        ) :
            DateFilter(
                R.string.time_filter_custom_label,
                check = { message: Message ->
                    message.timestamp in (timestampStart ?: System.currentTimeMillis()) until (timestampEnd
                        ?: System.currentTimeMillis())
                }
            ) {
            override fun getLabel(context: Context): String? {
                return context.getString(
                    R.string.time_filter_custom_label,
                    timestampStart?.toFormattedDate(context)
                        ?: context.getString(R.string.time_filter_custom_not_set_label),
                    timestampEnd?.toFormattedDate(context)
                        ?: context.getString(R.string.time_filter_custom_not_set_label)
                )
            }

        }

        companion object {
            val allFilterList = listOf<DateFilter>(
                WithinThreeDays,
                WithinThisWeek,
                WithinThisMonth,
                MoreThanAMonth
            )
        }
    }
}
