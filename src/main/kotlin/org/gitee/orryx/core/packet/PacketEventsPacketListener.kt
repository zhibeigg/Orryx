package org.gitee.orryx.core.packet

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage
import taboolib.common.platform.Ghost

@Ghost
class PacketEventsPacketListener : PacketListener {

    override fun onPacketReceive(event: PacketReceiveEvent) {
        // 用户代表玩家。
        val user = event.user
        // 确定它是什么类型的包。
        if (event.packetType != PacketType.Play.Client.CHAT_MESSAGE) return
        // 使用正确的包装器来处理此数据包。
        val chatMessage = WrapperPlayClientChatMessage(event)
        // 使用包装器的“getter”访问包装器中的数据
        val message = chatMessage.message
        // 检查消息是否为“ping”
        if (message.equals("ping", ignoreCase = true)) {
            // 用“pong”消息响应客户端。
            user.sendMessage("pong")
        }
    }

}