package com.ojhdtapp.parabox.domain.model.message_content

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(val latitude: Double, val longitude: Double, val name: String?, val description: String?) :
    MessageContent {
    @IgnoredOnParcel
    val type = MessageContent.LOCATION
    override fun getContentString(): String {
        return "[位置]$name"
    }
}