package org.gitee.orryx.compat.protocollib

import com.comphenix.protocol.ProtocolLibrary
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.CompatGuard
import org.gitee.orryx.utils.PacketEventsPlugin
import org.gitee.orryx.utils.ProtocolLibPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.util.ReloadAwareLazy

object ProtocolLibHook {

    val offSpeedFovChange: Boolean by ReloadAwareLazy(Orryx.config) {
        Orryx.config.getBoolean("OffSpeedFovChange", true)
    }

    @Awake(LifeCycle.ENABLE)
    private fun enable() {
        if (!ProtocolLibPlugin.isEnabled || PacketEventsPlugin.isEnabled) return
        CompatGuard.linkageFallback("ProtocolLib", {}) {
            ProtocolLibrary.getProtocolManager().addPacketListener(FovModifierPacketListener())
        }
    }
}
