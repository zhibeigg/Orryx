package org.gitee.orryx.dao.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.deserializeToItemStack
import taboolib.platform.util.serializeToByteArray
import java.util.*

object ItemStackSerializer : KSerializer<ItemStack> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("orryx.ItemStack", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ItemStack) {
        val bytes = value.serializeToByteArray(true)
        val base64 = Base64.getEncoder().encodeToString(bytes)
        encoder.encodeString(base64)
    }

    override fun deserialize(decoder: Decoder): ItemStack {
        val base64 = decoder.decodeString()
        val bytes = Base64.getDecoder().decode(base64)
        return bytes.deserializeToItemStack(true)
    }
}

val nullableItemStackSerializer = ItemStackSerializer.nullable