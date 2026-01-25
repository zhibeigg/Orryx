package org.gitee.orryx.core

import org.bukkit.entity.Player
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.util.ReloadAwareLazy
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.onlinePlayers


object GameManager {

    private val disabledHunger by ReloadAwareLazy(Orryx.config) { Orryx.config.getBoolean("DisableHunger") }
    var shutdown: Boolean = false

    private const val ORRYX_JOB_ATTRIBUTE = "ORRYX@JOB@ATTRIBUTE"

    @SubscribeEvent
    private fun onPlayerJoin(e: PlayerJoinEvent) {
        initTimeoutFlag(e.player)
        updateJobAttribute(e.player)
    }

    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerJoinEvent) {
        cancelTimeoutFlag(e.player)
    }

    private fun initTimeoutFlag(player: Player) {
        player.orryxProfile().thenAccept {
            it.flags.forEach { (key, flag) ->
                flag.init(player, key)
            }
        }
    }

    private fun cancelTimeoutFlag(player: Player) {
        player.orryxProfile().thenAccept {
            it.flags.forEach { (key, flag) ->
                flag.cancel(player, key)
            }
        }
    }

    @Reload(2)
    private fun updateAttributes() {
        onlinePlayers.forEach {
            updateJobAttribute(it)
        }
    }

    @SubscribeEvent
    private fun onJobChange(e: OrryxPlayerJobChangeEvents.Post) {
        updateJobAttribute(e.player)
    }

    @SubscribeEvent
    private fun onLevelUp(e: OrryxPlayerJobLevelEvents.Up) {
        updateJobAttribute(e.player)
    }

    @SubscribeEvent
    private fun onLevelDown(e: OrryxPlayerJobLevelEvents.Down) {
        updateJobAttribute(e.player)
    }

    private fun updateJobAttribute(player: Player) {
        player.job {
            IAttributeBridge.INSTANCE.addAttribute(player, ORRYX_JOB_ATTRIBUTE, it.getAttributes())
        }.thenAccept {
            if (it == null) {
                IAttributeBridge.INSTANCE.removeAttribute(player, ORRYX_JOB_ATTRIBUTE)
            }
        }
    }

    @SubscribeEvent(EventPriority.MONITOR)
    private fun hunger(e: FoodLevelChangeEvent) {
        if (disabledHunger) {
            e.isCancelled = true
            if (MinecraftVersion.isHigher(MinecraftVersion.V1_16)) {
                e.entity.foodLevel = 20
                e.entity.saturation = 20F
            }
        }
    }

    @Awake(LifeCycle.DISABLE)
    private fun disabled() {
        consoleMessage("&e┣&7检测到关闭服务器 Orryx 开始关闭流程")
        ScriptManager.terminateAllSkills()
        consoleMessage("&e┣&7终止所有玩家技能 &a√")
        IManaManager.closeThread()
        consoleMessage("&e┣&7 Mana 线程已关闭 &a√")
        ISpiritManager.closeThread()
        consoleMessage("&e┣&7 Spirit 线程已关闭 &a√")
        consoleMessage("&e┣&7等待协程完成...")
        OrryxAPI.shutdownScopes(timeout = 5)
        consoleMessage("&e┣&7协程域终止 &a√")
        shutdown = true
        consoleMessage("&e┣&7 Storage 禁止异步 &a√")
    }
}