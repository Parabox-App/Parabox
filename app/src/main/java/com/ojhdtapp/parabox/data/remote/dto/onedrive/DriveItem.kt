package com.ojhdtapp.parabox.data.remote.dto.onedrive

/**
 * @author laoyuyu
 * @date 2021/2/6
 */
data class DriveItem(
    val id: String,
    val driveType: String,
    val quota: Quota,
    val name: String
)

data class Quota(
    val total: Long,
    val used: Long,
    val deleted: Long,
    val remaining: Long,
    val state: String
)