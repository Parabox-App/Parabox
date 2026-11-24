package com.ojhdt.parabox.extension.tdlib.mapper

import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

/**
 * Lazy on-demand resource URI builder/parser for TDLib media and avatars.
 *
 * Format: `parabox-ext://<sessionId>/<resourceId>`
 * Supported resource IDs:
 * - `avatar/user/<userId>`
 * - `avatar/chat/<chatId>`
 * - `remote/<remoteFileId>`
 */
object TdResourceUri {
    const val SCHEME = "parabox-ext"
    const val PREFIX_AVATAR_USER = "avatar/user/"
    const val PREFIX_AVATAR_CHAT = "avatar/chat/"
    const val PREFIX_REMOTE_FILE = "remote/"

    fun userAvatar(sessionId: String, userId: Long): ParaboxResourceInfo {
        if (sessionId.isBlank() || userId == 0L) return ParaboxResourceInfo.ParaboxEmptyInfo
        return ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(
            "$SCHEME://$sessionId/$PREFIX_AVATAR_USER$userId"
        )
    }

    fun chatAvatar(sessionId: String, chatId: Long): ParaboxResourceInfo {
        if (sessionId.isBlank() || chatId == 0L) return ParaboxResourceInfo.ParaboxEmptyInfo
        return ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(
            "$SCHEME://$sessionId/$PREFIX_AVATAR_CHAT$chatId"
        )
    }

    fun remoteFile(sessionId: String, remoteFileId: String?): ParaboxResourceInfo {
        val id = remoteFileId?.trim().orEmpty()
        if (sessionId.isBlank() || id.isEmpty()) return ParaboxResourceInfo.ParaboxEmptyInfo
        return ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(
            "$SCHEME://$sessionId/$PREFIX_REMOTE_FILE$id"
        )
    }
}
