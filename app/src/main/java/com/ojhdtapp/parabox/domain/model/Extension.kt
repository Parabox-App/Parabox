package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension

data class Extension(
    val name: String,
    val pkgName: String,
    val versionName: String,
    val versionCode: Long,
    val ext: ParaboxExtension
)