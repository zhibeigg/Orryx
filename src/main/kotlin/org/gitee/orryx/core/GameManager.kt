package org.gitee.orryx.core

import org.bukkit.Bukkit
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.server.ServerCommandEvent
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.mana.IManaManager
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.module.chat.colored


object GameManager {

    private val disabledHunger
        get() = OrryxAPI.config.getBoolean("disableHunger")

    var shutdown: Boolean = false

    @SubscribeEvent(EventPriority.MONITOR)
    private fun hunger(e: FoodLevelChangeEvent) {
        if (disabledHunger) {
            e.isCancelled = true
            e.entity.foodLevel = 20
            e.entity.saturation = 20F
        }
    }

    @SubscribeEvent(EventPriority.MONITOR)
    private fun onServerCommand(e: ServerCommandEvent) {
        if (e.command.equals("stop", ignoreCase = true)) {
            e.isCancelled = true
            shutdownServer()
        }
    }

    fun shutdownServer() {
        info("&e┣&7检测到关闭服务器的命令 Orryx开始关闭流程".colored())
        IManaManager.closeThread()
        info("&e┣&7Mana线程已关闭 &a√".colored())
        shutdown = true
        info("&e┣&7Storage禁止异步 &a√".colored())
        ScriptManager.terminateAllSkills()
        info("&e┣&7终止所有玩家技能 &a√".colored())
        info("&e┣&7延迟2Tick后关闭服务器 &a√".colored())
        submit(delay = 2) {
            Bukkit.getServer().shutdown()
        }
    }

}