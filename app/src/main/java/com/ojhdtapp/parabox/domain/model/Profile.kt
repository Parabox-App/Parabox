package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Profile(
    val name: String,
    val avatar: String?,
    val avatarUri: String?,
    val id: Long?,
) : Parcelable