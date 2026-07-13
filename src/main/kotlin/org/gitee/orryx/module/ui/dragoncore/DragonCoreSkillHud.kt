package org.gitee.orryx.module.ui.dragoncore

import eos.moe.dragoncore.network.PacketSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillHud
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.module.ui.OwnerViewerIndex
import org.gitee.orryx.module.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.utils.*
import taboolib.common.function.debounce
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submit
import taboolib.platform.util.onlinePlayers
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class DragonCoreSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    private val debouncedUpdate = debounce(50L) { r: Result<IPlayerSkill?> ->
        submit { if (active) updateNow(r.getOrNull()) }
    }

    companion object {

        private val index = OwnerViewerIndex<DragonCoreSkillHud>({ it.owner.uniqueId }, { it.viewer.uniqueId })
        internal val dragonSkillHudMap = index.byOwner
        internal lateinit var skillHudConfiguration: YamlConfiguration

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is DragonCoreUIManager) return
            skillHudConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/dragoncore/OrryxSkillHUD.yml"))
            onlinePlayers.forEach {
                PacketSender.sendYaml(it, "gui/OrryxSkillHUD.yml", skillHudConfiguration)
            }
            dragonSkillHudMap.toList().forEach { (_, u) ->
                u.values.toList().forEach {
                    it.open()
                }
            }
        }

        fun getViewerHud(player: Player): DragonCoreSkillHud? = index.getViewer(player.uniqueId)

        fun closeForPlayer(player: Player) {
            index.removePlayer(player.uniqueId).forEach { it.deactivate(true) }
        }
    }

    private var active = false
    private var generation = 0L

    private fun isCurrent(expectedGeneration: Long): Boolean {
        return active && generation == expectedGeneration && getViewerHud(viewer) === this
    }

    override fun update(skill: IPlayerSkill?) {
        if (active) debouncedUpdate(Result.success(skill))
    }

    private fun updateNow(skill: IPlayerSkill?) {
        val expectedGeneration = generation
        if (!isCurrent(expectedGeneration)) return
        if (skill != null) {
            PacketSender.sendSyncPlaceholder(viewer, mapOf(
                "Orryx_bind_cooldown_${skill.key}" to skillCooldownMap[owner.uniqueId]?.get(skill.key)?.getCountdown(owner).toString()
            ))
        } else {
            owner.keySetting { keySetting ->
                owner.job { job ->
                    job.bindSkills { bindSkills ->
                        if (!isCurrent(expectedGeneration)) return@bindSkills
                        val keys = bindKeys()
                        PacketSender.sendSyncPlaceholder(viewer, mapOf(
                            "Orryx_bind_keys" to keys.joinToString("<br>") { it.key },
                            "Orryx_bind_player_keys" to keys.joinToString("<br>") { keySetting.bindKeyMap[it] ?: "none" },
                            "Orryx_bind_skills" to keys.joinToString("<br>") { bindSkills[it]?.key ?: "none" },
                            "Orryx_bind_skills_Icon" to keys.joinToString("<br>") { bindSkills[it]?.getIcon() ?: "none" },
                            "Orryx_bind_cooldowns" to keys.joinToString("<br>") { bindSkills[it]?.key?.let { skill -> skillCooldownMap[owner.uniqueId]?.get(skill)?.getCountdown(owner) }.toString() },
                            "Orryx_bind_skills_mana" to keys.joinToString("<br>") { bindSkills[it]?.parameter()?.manaValue()?.toString() ?: "0" },
                        ))
                    }
                }
            }
        }
    }

    override fun open() {
        remove(true)
        generation++
        active = true
        index.register(this)
        PacketSender.sendOpenHud(viewer, "OrryxSkillHUD")
        update()
    }

    override fun close() {
        remove(true)
    }

    private fun deactivate(close: Boolean) {
        generation++
        active = false
        if (close) PacketSender.sendRunFunction(viewer, "OrryxSkillHUD", "方法.关闭界面;", false)
    }

    protected open fun remove(close: Boolean = true) {
        val indexed = index.getViewer(viewer.uniqueId)
        linkedSetOf<DragonCoreSkillHud>().apply {
            indexed?.let(::add)
            add(this@DragonCoreSkillHud)
        }.forEach { hud ->
            index.remove(hud)
            hud.deactivate(close)
        }
    }
}
