package org.gitee.orryx.module.ui.arcartx

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillHud
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.module.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.api.ArcartXAPI
import taboolib.common.function.debounce
import taboolib.common.platform.function.getDataFolder
import taboolib.common.util.unsafeLazy
import java.io.File
import java.util.*

open class ArcartXSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    private val debouncedUpdate = debounce(50L) { r: Result<IPlayerSkill?> ->
        updateNow(r.getOrNull())
    }

    companion object {

        /**
         * owner, viewer, [ArcartXSkillHud]
         */
        internal val arcartxSkillHudMap by unsafeLazy { hashMapOf<UUID, MutableMap<UUID, ArcartXSkillHud>>() }
        internal lateinit var skillHudConfiguration: YamlConfiguration

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is ArcartXUIManager) return
            skillHudConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/arcartx/OrryxSkillHUD.yml"))
            arcartxSkillHudMap.toList().forEach { (_, u) ->
                u.values.toList().forEach {
                    it.open()
                }
            }
        }

        fun getViewerHud(player: Player): ArcartXSkillHud? {
            return arcartxSkillHudMap.firstNotNullOfOrNull {
                it.value[player.uniqueId]
            }
        }
    }

    override fun update(skill: IPlayerSkill?) {
        debouncedUpdate(Result.success(skill))
    }

    private fun updateNow(skill: IPlayerSkill?) {
        val networkSender = ArcartXAPI.getNetworkSender()
        if (skill != null) {
            val cooldown = skillCooldownMap[owner.uniqueId]?.get(skill.key)
            networkSender.sendServerVariable(viewer, "Orryx_bind_cooldown_${skill.key}", cooldown?.getCountdown(owner).toString())
        } else {
            owner.keySetting { keySetting ->
                owner.job { job ->
                    job.bindSkills { bindSkills ->
                        val keys = bindKeys()
                        networkSender.sendMultipleServerVariable(viewer, mapOf(
                            "Orryx_bind_keys" to keys.joinToString("<br>") { it.key },
                            "Orryx_bind_player_keys" to keys.joinToString("<br>") { keySetting.bindKeyMap[it] ?: "none" },
                            "Orryx_bind_skills" to keys.joinToString("<br>") { bindSkills[it]?.key ?: "none" },
                            "Orryx_bind_skills_Icon" to keys.joinToString("<br>") { bindSkills[it]?.getIcon() ?: "none" },
                            "Orryx_bind_cooldowns" to keys.joinToString("<br>") { bindSkills[it]?.key?.let { skill -> skillCooldownMap[owner.uniqueId]?.get(skill)?.getCountdown(owner) }.toString() },
                            "Orryx_bind_skills_mana" to keys.joinToString("<br>") { bindSkills[it]?.parameter()?.manaValue()?.toString() ?: "0" }
                        ))
                    }
                }
            }
        }
    }

    override fun open() {
        remove(true)
        ArcartXAPI.getUIRegistry().open(viewer, "OrryxSkillHUD")
        update()
        arcartxSkillHudMap.getOrPut(owner.uniqueId) { hashMapOf() }[viewer.uniqueId] = this
    }

    override fun close() {
        remove(true)
    }

    protected open fun remove(close: Boolean = true) {
        arcartxSkillHudMap.forEach {
            val iterator = it.value.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next.key == viewer.uniqueId) {
                    iterator.remove()
                    if (close) {
                        ArcartXAPI.getUIRegistry().close(next.value.viewer, "OrryxSkillHUD")
                    }
                }
            }
        }
        if (arcartxSkillHudMap[owner.uniqueId]?.isEmpty() == true) {
            arcartxSkillHudMap.remove(owner.uniqueId)
        }
    }
}
