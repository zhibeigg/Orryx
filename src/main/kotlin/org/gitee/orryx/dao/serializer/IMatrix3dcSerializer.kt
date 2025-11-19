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
import org.joml.Matrix3d
import org.joml.Matrix3dc

object IMatrix3dcSerializer : KSerializer<Matrix3dc> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("orryx.Matrix", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Matrix3dc) {
        encoder.encodeStructure(descriptor) {
            encodeDoubleElement(descriptor, 0, value.m00())
            encodeDoubleElement(descriptor, 1, value.m01())
            encodeDoubleElement(descriptor, 2, value.m02())
            encodeDoubleElement(descriptor, 3, value.m10())
            encodeDoubleElement(descriptor, 4, value.m12())
            encodeDoubleElement(descriptor, 5, value.m12())
            encodeDoubleElement(descriptor, 6, value.m20())
            encodeDoubleElement(descriptor, 7, value.m22())
            encodeDoubleElement(descriptor, 8, value.m22())
            endStructure(descriptor)
        }
    }

    override fun deserialize(decoder: Decoder): Matrix3dc {
        return decoder.decodeStructure(descriptor) {
            Matrix3d(
                decodeDoubleElement(descriptor, 0), decodeDoubleElement(descriptor, 1), decodeDoubleElement(descriptor, 2),
                decodeDoubleElement(descriptor, 3), decodeDoubleElement(descriptor, 4), decodeDoubleElement(descriptor, 5),
                decodeDoubleElement(descriptor, 6), decodeDoubleElement(descriptor, 7), decodeDoubleElement(descriptor, 8)
            )
        }
    }
}

val nullIMatrix3dcSerializer get() = IMatrix3dcSerializer.nullable