package org.gitee.orryx.compat.protocollib

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import taboolib.common.platform.Ghost
import taboolib.platform.BukkitPlugin

@Ghost
class FovModifierPacketListener: PacketAdapter(BukkitPlugin.getInstance(), PacketType.Play.Server.ABILITIES) {

    override fun onPacketSending(e: PacketEvent) {
        if (!ProtocolLibHook.offSpeedFovChange) return
        if (e.packetType != PacketType.Play.Server.ABILITIES) return
        e.packet.float.write(1, 0f)
    }
}