package com.ojhdtapp.paraboxdevelopmentkit.v2

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ParaboxMediaHelper {
    /**
     * Expose a local file from an extension APK to the Parabox host app using FileProvider
     * with temporary read-only URI permission (zero-copy across Binder boundaries).
     */
    fun shareFileWithHost(
        context: Context,
        authority: String,
        file: File,
        hostPackageName: String,
    ): Uri {
        val uri = FileProvider.getUriForFile(context, authority, file)
        if (hostPackageName.isNotBlank()) {
            context.grantUriPermission(
                hostPackageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        return uri
    }
}
