package com.ojhdtapp.parabox.domain.model

data class RecentQuery(
    val value: String,
    val timestamp: Long,
    val id: Long = 0,
)
