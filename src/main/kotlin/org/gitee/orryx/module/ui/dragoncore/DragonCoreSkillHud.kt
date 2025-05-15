package org.gitee.orryx.module.ui.dragoncore

import eos.moe.dragoncore.network.PacketSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillHud
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.module.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.module.ui.germplugin.GermPluginSkillHud
import org.gitee.orryx.utils.bindKeys
import org.gitee.orryx.utils.bindSkills
import org.gitee.orryx.utils.job
import taboolib.common.platform.function.getDataFolder
import taboolib.common.util.unsafeLazy
import taboolib.platform.util.onlinePlayers
import java.io.File
import java.util.*

open class DragonCoreSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    companion object {

        /**
         * owner, viewer, [GermPluginSkillHud]
         */
        internal val dragonSkillHudMap by unsafeLazy { hashMapOf<UUID, MutableMap<UUID, DragonCoreSkillHud>>() }
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

        fun getViewerHud(player: Player): DragonCoreSkillHud? {
            return dragonSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }
    }

    override fun update(skill: IPlayerSkill?) {
        if (skill != null) {
            PacketSender.sendSyncPlaceholder(viewer, mapOf(
                "Orryx_bind_cooldown_${skill.key}" to skillCooldownMap[owner.uniqueId]?.get(skill.key)?.getCountdown(owner).toString()
            ))
        } else {
            owner.job { job ->
                job.bindSkills { bindSkills ->
                    val keys = bindKeys()
                    PacketSender.sendSyncPlaceholder(viewer, mapOf(
                        "Orryx_bind_keys" to keys.joinToString("<br>") { it.key },
                        "Orryx_bind_skills" to keys.joinToString("<br>") { bindSkills[it]?.key ?: "none" },
                        "Orryx_bind_cooldowns" to keys.joinToString("<br>") { bindSkills[it]?.key?.let { skill -> skillCooldownMap[owner.uniqueId]?.get(skill)?.getCountdown(owner) }.toString() }
                    ))
                }
            }
        }
    }

    override fun open() {
        remove(true)
        PacketSender.sendOpenHud(viewer, "OrryxSkillHUD")
        update()
        dragonSkillHudMap.getOrPut(owner.uniqueId) { hashMapOf() }[viewer.uniqueId] = this
    }

    override fun close() {
        remove(true)
    }

    protected open fun remove(close: Boolean = true) {
        dragonSkillHudMap.forEach {
            val iterator = it.value.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.key == viewer.uniqueId) {
                    iterator.remove()
                    if (close) {
                        PacketSender.sendRunFunction(next.value.viewer, "OrryxSkillHUD", "方法.关闭界面;", false)
                    }
                }
            }
        }
        if (dragonSkillHudMap[owner.uniqueId]?.isEmpty() == true) {
            dragonSkillHudMap.remove(owner.uniqueId)
        }
    }
}