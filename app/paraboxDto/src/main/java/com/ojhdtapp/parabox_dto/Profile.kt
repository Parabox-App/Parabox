package com.ojhdtapp.parabox_dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Profile(
    val name: String,
    val avatar: String?
) : Parcelable