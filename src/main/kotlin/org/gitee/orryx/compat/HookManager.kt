package org.gitee.orryx.compat

import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.chat.colored

object HookManager {

    @Awake(LifeCycle.ENABLE)
    private fun load() {
        info("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
        AdyeshachPlugin.load()
        DragonCorePlugin.load()
        DragonArmourersPlugin.load()
        GermPluginPlugin.load()
        MythicMobsPlugin.load()
        RedisChannelPlugin.load()
        NodensPlugin.load()
        AttributePlusPlugin.load()
        PacketEventsPlugin.load()
        ProtocolLibPlugin.load()
        GDDTitlePlugin.load()
        PlaceholderAPIPlugin.load()
        GlowAPIPlugin.load()
        DungeonPlusPlugin.load()
    }
}