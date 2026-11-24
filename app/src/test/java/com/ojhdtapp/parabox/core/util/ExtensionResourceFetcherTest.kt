package com.ojhdtapp.parabox.core.util

import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtensionResourceFetcherTest {

    @Test
    fun parseValidAndInvalidExtensionUris() {
        val parsedAvatar = ExtensionResourceUri.parse("parabox-ext://conn-12/avatar/user/42")
        assertEquals(
            ExtensionResourceUri.Parsed(
                sessionId = "conn-12",
                connectionId = 12L,
                resourceId = "avatar/user/42",
            ),
            parsedAvatar,
        )

        val parsedRemote = ExtensionResourceUri.parse("parabox-ext://conn-3/remote/AgACAgIAAxkBAA")
        assertEquals(
            ExtensionResourceUri.Parsed(
                sessionId = "conn-3",
                connectionId = 3L,
                resourceId = "remote/AgACAgIAAxkBAA",
            ),
            parsedRemote,
        )

        assertNull(ExtensionResourceUri.parse("https://example.com/pic.jpg"))
        assertNull(ExtensionResourceUri.parse("parabox-ext://bad-host/avatar/user/1"))
        assertNull(ExtensionResourceUri.parse("parabox-ext://conn-1/"))
        assertNull(ExtensionResourceUri.parse("parabox-ext://conn-1"))
    }

    @Test
    fun resolveDisplayAvatarFallbackForLegacyTdlibRows() {
        val tdPkg = "com.ojhdt.parabox.extension.tdlib"

        // 1. Legacy empty TDLib chat avatar -> lazy parabox-ext chat avatar
        val resolvedChat = ExtensionResourceUri.resolveDisplayAvatarInfo(
            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
            pkg = tdPkg,
            extensionId = 5L,
            uid = "-10012345",
            isChat = true,
        )
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-5/avatar/chat/-10012345"),
            resolvedChat,
        )

        // 2. Legacy empty TDLib user avatar -> lazy parabox-ext user avatar
        val resolvedUser = ExtensionResourceUri.resolveDisplayAvatarInfo(
            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
            pkg = tdPkg,
            extensionId = 5L,
            uid = "987654",
            isChat = false,
        )
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-5/avatar/user/987654"),
            resolvedUser,
        )

        // 3. Already-migrated lazy URL is preserved unchanged
        val existingUrl = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-5/avatar/user/987654")
        assertEquals(
            existingUrl,
            ExtensionResourceUri.resolveDisplayAvatarInfo(
                avatar = existingUrl,
                pkg = tdPkg,
                extensionId = 5L,
                uid = "987654",
                isChat = false,
            ),
        )

        // 4. Self contact sentinel does not fabricate a TDLib user avatar
        assertEquals(
            ParaboxResourceInfo.ParaboxEmptyInfo,
            ExtensionResourceUri.resolveDisplayAvatarInfo(
                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                pkg = tdPkg,
                extensionId = 5L,
                uid = "__parabox_self__",
                isChat = false,
            ),
        )

        // 5. Non-TDLib empty avatar stays empty
        assertEquals(
            ParaboxResourceInfo.ParaboxEmptyInfo,
            ExtensionResourceUri.resolveDisplayAvatarInfo(
                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                pkg = "com.example.other.extension",
                extensionId = 5L,
                uid = "42",
                isChat = false,
            ),
        )
    }
}
