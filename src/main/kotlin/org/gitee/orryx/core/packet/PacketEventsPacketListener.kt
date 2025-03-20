package org.gitee.orryx.core.packet

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import taboolib.common.platform.Ghost

@Ghost
class PacketEventsPacketListener : PacketListener {

    override fun onPacketSend(event: PacketSendEvent) {
    }

}