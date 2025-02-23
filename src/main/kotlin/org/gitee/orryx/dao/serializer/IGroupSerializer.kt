package org.gitee.orryx.dao.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.utils.DEFAULT

object IGroupSerializer : KSerializer<IGroup> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("orryx.IGroup", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IGroup) {
        val string = value.key
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): IGroup {
        return BindKeyLoaderManager.getGroup(decoder.decodeString()) ?: BindKeyLoaderManager.getGroup(DEFAULT)!!
    }

}