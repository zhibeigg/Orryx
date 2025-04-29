package org.gitee.orryx.compat.protocollib

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import taboolib.platform.BukkitPlugin

class FovModifierPacketListener: PacketAdapter(BukkitPlugin.getInstance(), PacketType.Play.Server.ABILITIES) {

    override fun onPacketReceiving(e: PacketEvent) {
        if (e.packetType != PacketType.Play.Server.ABILITIES) return
        e.packet.float.write(1, 0.1f)
    }
}