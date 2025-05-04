package org.gitee.orryx.compat.packetevents

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities
import taboolib.common.platform.Ghost

@Ghost
class FovModifierPacketListener: PacketListener {

    override fun onPacketSend(e: PacketSendEvent) {
        if (!PacketEventsHook.offSpeedFovChange) return
        if (e.packetType != PacketType.Play.Server.PLAYER_ABILITIES) return
        val packet = WrapperPlayServerPlayerAbilities(e)
        packet.fovModifier = 0f
        e.markForReEncode(true)
        packet.write()
        e.byteBuf = packet.readByte()
    }
}