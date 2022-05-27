package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageProfile(val name: String,
val avatar: ByteArray?) : Parcelable
