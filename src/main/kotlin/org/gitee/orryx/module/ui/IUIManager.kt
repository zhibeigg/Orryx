package org.gitee.orryx.module.ui

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillBindKeyEvent
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents
import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillUnBindKeyEvent
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.bukkit.BukkitSkillHud.Companion.bukkitSkillHudMap
import org.gitee.orryx.module.ui.bukkit.BukkitUIManager
import org.gitee.orryx.module.ui.dragoncore.DragonCoreSkillHud.Companion.dragonSkillHudMap
import org.gitee.orryx.module.ui.dragoncore.DragonCoreUIManager
import org.gitee.orryx.module.ui.germplugin.GermPluginSkillHud.Companion.germSkillHudMap
import org.gitee.orryx.module.ui.germplugin.GermPluginUIManager
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.common5.cdouble
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration
import java.util.*

interface IUIManager {

    companion object {

        /**
         * owner, skill, [Cooldown]
         */
        internal val skillCooldownMap by unsafeLazy { hashMapOf<UUID, MutableMap<String, Cooldown>>() }

        class Cooldown(val skill: String, val max: Long) {

            fun percent(player: Player): Double {
                return (max.cdouble - SkillTimer.getCountdown(player, skill).cdouble) / max.cdouble
            }

            fun getOverStamp(player: Player): Long {
                return SkillTimer.getCooldownEntry(adaptPlayer(player), skill)?.overStamp ?: 0
            }

            fun getRemaining(player: Player): Long {
                return SkillTimer.getCooldownEntry(adaptPlayer(player), skill)?.remaining ?: 0
            }

            fun getCountdown(player: Player): Long {
                return SkillTimer.getCooldownEntry(adaptPlayer(player), skill)?.countdown ?: 0
            }

            fun isOver(player: Player): Boolean {
                return SkillTimer.getCountdown(player, skill) <= 0L
            }
        }

        private fun updateAll(player: Player, skill: IPlayerSkill? = null) {
            bukkitSkillHudMap[player.uniqueId]?.forEach { (_, u) ->
                u.update(skill)
            }
            germSkillHudMap[player.uniqueId]?.forEach { (_, u) ->
                u.update(skill)
            }
            dragonSkillHudMap[player.uniqueId]?.forEach { (_, u) ->
                u.update(skill)
            }
        }

        @SubscribeEvent
        private fun cooldown(e: OrryxPlayerSkillCooldownEvents.Set.Post) {
            val cooldownMap = skillCooldownMap.getOrPut(e.player.uniqueId) { hashMapOf() }
            val iterator = cooldownMap.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.isOver(e.player)) {
                    iterator.remove()
                }
            }
            cooldownMap[e.skill.key] = Cooldown(e.skill.key, e.amount)
            updateAll(e.player, e.skill)
        }

        @SubscribeEvent
        private fun cooldown(e: OrryxPlayerSkillCooldownEvents.Increase.Post) {
            updateAll(e.player, e.skill)
        }

        @SubscribeEvent
        private fun cooldown(e: OrryxPlayerSkillCooldownEvents.Reduce.Post) {
            updateAll(e.player, e.skill)
        }

        @SubscribeEvent
        private fun bind(e: OrryxPlayerSkillBindKeyEvent.Post) {
            updateAll(e.player)
        }

        @SubscribeEvent
        private fun unbind(e: OrryxPlayerSkillUnBindKeyEvent.Post) {
            updateAll(e.player)
        }

        @SubscribeEvent
        private fun changeJob(e: OrryxPlayerJobChangeEvents.Post) {
            updateAll(e.player)
        }

        @SubscribeEvent
        private fun quit(e: PlayerQuitEvent) {
            skillCooldownMap.remove(e.player.uniqueId)
        }

        private val type
            get() = Orryx.config.getString("UI.use", "bukkit")!!.uppercase()

        internal lateinit var INSTANCE: IUIManager

        @Awake(LifeCycle.ENABLE)
        private fun awake() {
            INSTANCE = when (type) {
                "BUKKIT" -> {
                    info(("&e┣&7已选择原版UI &a√").colored())
                    BukkitUIManager()
                }
                "DRAGONCORE", "DRAGON" -> {
                    if (DragonCorePlugin.isEnabled) {
                        info(("&e┣&7已选择龙核UI &a√").colored())
                        DragonCoreUIManager()
                    } else {
                        info(("&e┣&7因为未检测到DragonCore，已自动选择BUKKIT UI &a√").colored())
                        BukkitUIManager()
                    }
                }
                "GERMPLUGIN", "GERM" -> {
                    if (GermPluginPlugin.isEnabled) {
                        info(("&e┣&7已选择萌芽UI &a√").colored())
                        GermPluginUIManager()
                    } else {
                        info(("&e┣&7因为未检测到GermPlugin，已自动选择BUKKIT UI &a√").colored())
                        BukkitUIManager()
                    }
                }
                else -> error("未知的UI类型: $type")
            }
        }

        @Reload(1)
        private fun reload() {
            INSTANCE.reload()
        }
    }

    /**
     * UI配置
     * */
    val config: Configuration

    /**
     * 创建技能UI
     * @param viewer 浏览者
     * @param owner 拥有者
     * @return 技能UI[ISkillUI]
     * */
    fun createSkillUI(viewer: Player, owner: Player): ISkillUI

    /**
     * 创建技能HUD
     * @param viewer 浏览者
     * @param owner 拥有者
     * @return 技能HUD[ISkillHud]
     * */
    fun createSkillHUD(viewer: Player, owner: Player): ISkillHud

    /**
     * 获取技能HUD
     * @param viewer 浏览者
     * @return 技能HUD[ISkillHud]
     * */
    fun getSkillHUD(viewer: Player): ISkillHud?

    /**
     * 重载
     * */
    fun reload()
}