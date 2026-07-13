package org.gitee.orryx.compat.packetevents

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.CompatGuard
import org.gitee.orryx.utils.PacketEventsPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.util.ReloadAwareLazy

object PacketEventsHook {

    val offSpeedFovChange: Boolean by ReloadAwareLazy(Orryx.config) {
        Orryx.config.getBoolean("OffSpeedFovChange", true)
    }

    /** 仅使用外部 PacketEvents 插件持有的全局 API，不 setAPI、不 init/terminate。 */
    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        if (!PacketEventsPlugin.isEnabled) return
        CompatGuard.linkageFallback("PacketEvents", {}) {
            PacketEvents.getAPI().eventManager.registerListener(
                FovModifierPacketListener(),
                PacketListenerPriority.NORMAL,
            )
        }
    }
}
