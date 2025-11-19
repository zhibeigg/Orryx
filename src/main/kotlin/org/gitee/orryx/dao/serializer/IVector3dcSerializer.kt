package org.gitee.orryx.dao.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.joml.Vector3dc

object IVector3dcSerializer : KSerializer<Vector3dc> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("orryx.Vector", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Vector3dc) {
        encoder.encodeStructure(descriptor) {
            encodeDoubleElement(descriptor, 0, value.x())
            encodeDoubleElement(descriptor, 1, value.y())
            encodeDoubleElement(descriptor, 2, value.z())
            endStructure(descriptor)
        }
    }

    override fun deserialize(decoder: Decoder): Vector3dc {
        return decoder.decodeStructure(descriptor) {
            AbstractVector(decodeDoubleElement(descriptor, 0), decodeDoubleElement(descriptor, 1), decodeDoubleElement(descriptor, 2))
        }
    }
}

val nullIVector3dcSerializer get() = IVector3dcSerializer.nullable