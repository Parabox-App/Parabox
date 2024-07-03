package com.ojhdtapp.parabox.domain.model

data class ChangeLog(
    val version: String,
    val timestamp: Long,
    val contentMdCn: String,
    val contentMdEn: String,
) {
    constructor() : this(
        "",
        0L,
        "",
        ""
    )
}
