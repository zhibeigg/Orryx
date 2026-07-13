package org.gitee.orryx.core

import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.thenApplyMain
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.configuration.util.ReloadAwareLazy
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.onlinePlayers


object GameManager {

    private val disabledHunger by ReloadAwareLazy(Orryx.config) { Orryx.config.getBoolean("DisableHunger") }
    private val disabledCombust by ReloadAwareLazy(Orryx.config) { Orryx.config.getBoolean("DisabledCombust") }
    var shutdown: Boolean = false

    private const val ORRYX_JOB_ATTRIBUTE = "ORRYX@JOB@ATTRIBUTE"

    @SubscribeEvent
    private fun onPlayerJoin(e: PlayerJoinEvent) {
        initTimeoutFlag(e.player)
        updateJobAttribute(e.player)
    }

    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerQuitEvent) {
        cancelTimeoutFlag(e.player)
    }

    private fun initTimeoutFlag(player: Player) {
        player.orryxProfile().thenApplyMain { profile ->
            profile.flags.forEach { (key, flag) -> flag.init(player, key) }
        }
    }

    private fun cancelTimeoutFlag(player: Player) {
        player.orryxProfile().thenApplyMain { profile ->
            profile.flags.forEach { (key, flag) -> flag.cancel(player, key) }
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
    private fun onLevelUp(e: OrryxPlayerJobLevelEvents.Up.Post) {
        updateJobAttribute(e.player)
    }

    @SubscribeEvent
    private fun onLevelDown(e: OrryxPlayerJobLevelEvents.Down.Post) {
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

    @SubscribeEvent
    private fun combust(e: EntityCombustEvent) {
        if (disabledCombust && e.entity.type == EntityType.ZOMBIE) {
            e.isCancelled = true
        }
    }

    @Awake(LifeCycle.DISABLE)
    private fun disabled() {
        shutdown = true
        consoleMessage("&e┣&7检测到关闭服务器，Orryx 开始非阻塞关闭流程")
        ScriptManager.terminateAllSkills()
        IManaManager.closeThread()
        ISpiritManager.closeThread()
        consoleMessage("&e┣&7已停止技能与资源恢复任务 &a√")

        val shutdownFailures = mutableListOf<Throwable>()
        PersistenceManager.shutdown()
            .handle { _, throwable ->
                throwable?.let {
                    shutdownFailures += it
                    consoleMessage("&e┣&c持久化队列 flush 失败: ${it.message}")
                    it.printStackTrace()
                }
                Unit
            }
            .thenCompose { ISyncCacheManager.INSTANCE.closeAsync() }
            .handle { _, throwable ->
                throwable?.let {
                    shutdownFailures += it
                    consoleMessage("&e┣&c同步缓存关闭失败: ${it.message}")
                    it.printStackTrace()
                }
                Unit
            }
            .thenCompose { IStorageManager.INSTANCE.closeAsync() }
            .whenComplete { _, throwable ->
                throwable?.let {
                    shutdownFailures += it
                    consoleMessage("&e┣&c数据库关闭失败: ${it.message}")
                    it.printStackTrace()
                }
                MemoryCache.printStats()
                OrryxAPI.shutdownScopes()
                if (shutdownFailures.isEmpty()) {
                    consoleMessage("&e┣&7Orryx 异步资源关闭流程结束 &a√")
                } else {
                    consoleMessage("&e┣&cOrryx 异步资源关闭完成，但有 ${shutdownFailures.size} 个阶段失败 &4×")
                }
            }
    }
}