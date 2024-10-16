package com.ojhdtapp.parabox.ui.navigation

import android.os.Bundle
import android.os.Parcel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object BundleSerializer : KSerializer<Bundle> {
    private val byteArraySerializer = ByteArraySerializer()
    override val descriptor: SerialDescriptor = byteArraySerializer.descriptor

    override fun serialize(encoder: Encoder, value: Bundle) {
        encoder.encodeSerializableValue(byteArraySerializer, value.toByteArray())
    }

    private fun Bundle.toByteArray(): ByteArray {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(this)
            return parcel.marshall()
        } finally {
            parcel.recycle()
        }
    }

    override fun deserialize(decoder: Decoder): Bundle =
        decoder.decodeSerializableValue(byteArraySerializer).toBundle()

    private fun ByteArray.toBundle(): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(this, 0, size)
            parcel.setDataPosition(0)
            return requireNotNull(parcel.readBundle())
        } finally {
            parcel.recycle()
        }
    }
}