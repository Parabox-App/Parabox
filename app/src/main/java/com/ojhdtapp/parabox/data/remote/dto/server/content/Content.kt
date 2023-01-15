package com.ojhdtapp.parabox.data.remote.dto.server.content

sealed interface Content {
    val type: Int
}

data class Text(
    val text: String,
    override val type: Int = 0
) : Content

data class Image(
    val url: String,
    val cloud_type: Int,
    val cloud_id: String,
    val file_name: String,
    override val type: Int = 1
) : Content