package org.gitee.orryx.dao.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.utils.DEFAULT

object IBindKeySerializer : KSerializer<IBindKey> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("orryx.IBindKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IBindKey) {
        val string = value.key
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): IBindKey {
        val string = decoder.decodeString()
        return BindKeyLoaderManager.getBindKey(string) ?: BindKeyLoaderManager.getBindKey(DEFAULT)!!
    }

}