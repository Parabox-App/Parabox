package com.ojhdtapp.parabox.data.local

import android.os.Bundle

open class ExtensionInfo(
    open val alias: String,
    open val pkg: String,
    open val name: String,
    open val version: String,
    open val versionCode: Long,
    open val extra: Bundle,
    open val extensionId: Long,
)
