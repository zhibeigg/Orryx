package org.gitee.orryx.compat.packetevents

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketListenerPriority
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.CompatGuard
import org.gitee.orryx.utils.PacketEventsPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.util.ReloadAwareLazy

object PacketEventsHook {

    private var listener: PacketListenerCommon? = null

    val offSpeedFovChange: Boolean by ReloadAwareLazy(Orryx.config) {
        Orryx.config.getBoolean("OffSpeedFovChange", true)
    }

    /** 仅使用外部 PacketEvents 插件持有的全局 API，不 setAPI、不 init/terminate。 */
    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        if (!PacketEventsPlugin.isEnabled || listener != null) return
        listener = CompatGuard.linkageFallback("PacketEvents", { null }) {
            PacketEvents.getAPI().eventManager.registerListener(
                FovModifierPacketListener(),
                PacketListenerPriority.NORMAL,
            )
        }
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        val registered = listener ?: return
        listener = null
        CompatGuard.linkageFallback("PacketEvents 注销", {}) {
            PacketEvents.getAPI().eventManager.unregisterListener(registered)
        }
    }
}
