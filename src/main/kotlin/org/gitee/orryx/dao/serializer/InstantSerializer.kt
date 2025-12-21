package org.gitee.orryx.dao.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.Instant

object InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("orryx.Instant", PrimitiveKind.STRING)

    @Serializable
    data class SerializableInstant(val seconds: Long, val nanos: Int)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(Json.encodeToString(SerializableInstant(value.epochSecond, value.nano)))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Json.decodeFromString(decoder.decodeString())
    }
}

val nullInstantSerializer: nullable get() = InstantSerializer.nullable