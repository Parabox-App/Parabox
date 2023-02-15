package com.ojhdtapp.parabox.data.remote.dto.onedrive

data class MsalUploadSession(
    val uploadUrl: String,   // 上传路径
    val expirationDateTime: String, // 以 UTC 表示的上载会话过期的日期和时间。在此过期时间之前必须上载完整的文件文件。
    val nextExpectedRanges: List<String> // range 0-
)
