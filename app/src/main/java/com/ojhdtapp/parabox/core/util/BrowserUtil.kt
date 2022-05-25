package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

import android.content.Intent


object BrowserUtil {
    private val defaultColor = CustomTabColorSchemeParams.Builder()
//        .setToolbarColor(colorInt)
        .build()

    fun launchURL(context: Context, url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(defaultColor)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    fun composeEmail(context: Context, addresses: Array<String?>?, subject: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        context.startActivity(intent)
    }
}