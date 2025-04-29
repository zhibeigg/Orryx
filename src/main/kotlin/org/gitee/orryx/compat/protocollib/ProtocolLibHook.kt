package org.gitee.orryx.compat.protocollib

import com.comphenix.protocol.ProtocolLibrary
import org.gitee.orryx.utils.PacketEventsPlugin
import org.gitee.orryx.utils.ProtocolLibPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object ProtocolLibHook {

    @Awake(LifeCycle.ENABLE)
    private fun enable() {
        if (!ProtocolLibPlugin.isEnabled || PacketEventsPlugin.isEnabled) return
        ProtocolLibrary.getProtocolManager().addPacketListener(FovModifierPacketListener())
    }
}