package org.gitee.orryx.compat.packetevents

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.utils.ConfigLazy
import org.gitee.orryx.utils.PacketEventsPlugin
import org.gitee.orryx.utils.ReloadableLazy
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.platform.BukkitPlugin

object PacketEventsHook {

    val offSpeedFovChange: Boolean by ConfigLazy(Orryx.config) { Orryx.config.getBoolean("OffSpeedFovChange", true) }

    @Awake(LifeCycle.LOAD)
    private fun onLoad() {
        try {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(BukkitPlugin.getInstance()))
            PacketEvents.getAPI().settings.reEncodeByDefault(false).checkForUpdates(false)
            PacketEvents.getAPI().load()
            PacketEvents.getAPI().eventManager.registerListener(FovModifierPacketListener(), PacketListenerPriority.NORMAL)
        } catch (_: Throwable) {
        }
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        if (!PacketEventsPlugin.isEnabled) return
        PacketEvents.getAPI().init()
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        if (!PacketEventsPlugin.isEnabled) return
        PacketEvents.getAPI().terminate()
    }
}