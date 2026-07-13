package org.gitee.orryx.compat.protocollib

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketListener
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.CompatGuard
import org.gitee.orryx.utils.PacketEventsPlugin
import org.gitee.orryx.utils.ProtocolLibPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.util.ReloadAwareLazy

object ProtocolLibHook {

    private var listener: PacketListener? = null

    val offSpeedFovChange: Boolean by ReloadAwareLazy(Orryx.config) {
        Orryx.config.getBoolean("OffSpeedFovChange", true)
    }

    @Awake(LifeCycle.ENABLE)
    private fun enable() {
        if (!ProtocolLibPlugin.isEnabled || PacketEventsPlugin.isEnabled || listener != null) return
        CompatGuard.linkageFallback("ProtocolLib", {}) {
            FovModifierPacketListener().also {
                ProtocolLibrary.getProtocolManager().addPacketListener(it)
                listener = it
            }
        }
    }

    @Awake(LifeCycle.DISABLE)
    private fun disable() {
        val registered = listener ?: return
        listener = null
        CompatGuard.linkageFallback("ProtocolLib 注销", {}) {
            ProtocolLibrary.getProtocolManager().removePacketListener(registered)
        }
    }
}
