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
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

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
            ArcartXAPI.getUIRegistry().reload("OrryxSkillHUD", skillHudConfiguration)
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
        val uiRegistry = ArcartXAPI.getUIRegistry()

        if (skill != null) {
            val cooldown = skillCooldownMap[owner.uniqueId]?.get(skill.key)
            uiRegistry.sendPacket(
                viewer, "OrryxSkillHUD", "OrryxHudCooldown",
                mapOf(
                    "skill" to skill.key,
                    "cooldown" to cooldown?.getCountdown(owner).toString()
                )
            )
        } else {
            owner.keySetting { it }
                .thenCompose { keySetting ->
                    owner.job { it }.thenApply { keySetting to it }
                }
                .thenCompose { (keySetting, job) ->
                    if (job == null) return@thenCompose CompletableFuture.completedFuture(null)
                    job.bindSkills { bindSkills ->
                        val keys = bindKeys()

                        val bindSkillsData = keys.map { key ->
                            val bindSkill = bindSkills[key]
                            mapOf(
                                "key" to key.key,
                                "player_key" to (if (key.isClientKeyBind) key.defaultKey ?: "none" else keySetting.bindKeyMap[key] ?: "none"),
                                "skill" to (bindSkill?.key ?: "none"),
                                "icon" to (bindSkill?.getIcon() ?: "none"),
                                "cooldown" to (bindSkill?.key?.let { skillKey ->
                                    skillCooldownMap[owner.uniqueId]?.get(skillKey)?.getCountdown(owner)
                                }?.toString() ?: "0"),
                                "mana" to (bindSkill?.parameter()?.manaValue()?.toString() ?: "0")
                            )
                        }

                        uiRegistry.sendPacket(
                            viewer, "OrryxSkillHUD", "OrryxHudUpdate",
                            mapOf("bind_skills" to bindSkillsData)
                        )
                    }
                }.exceptionally { ex ->
                    warning("[Orryx] OrryxHudUpdate error: ${ex.message}")
                    ex.printStackTrace()
                    null
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
