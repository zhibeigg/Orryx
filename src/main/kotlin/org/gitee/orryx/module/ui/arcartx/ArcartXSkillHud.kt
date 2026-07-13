package org.gitee.orryx.module.ui.arcartx

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.module.ui.AbstractSkillHud
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.module.ui.OwnerViewerIndex
import org.gitee.orryx.module.ui.IUIManager.Companion.skillCooldownMap
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.api.ArcartXAPI
import taboolib.common.function.debounce
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

open class ArcartXSkillHud(override val viewer: Player, override val owner: Player): AbstractSkillHud(viewer, owner) {

    private val debouncedUpdate = debounce(50L) { r: Result<IPlayerSkill?> ->
        submit { if (active) updateNow(r.getOrNull()) }
    }

    companion object {

        private val index = OwnerViewerIndex<ArcartXSkillHud>({ it.owner.uniqueId }, { it.viewer.uniqueId })
        internal val arcartxSkillHudMap = index.byOwner
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

        fun getViewerHud(player: Player): ArcartXSkillHud? = index.getViewer(player.uniqueId)

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
                        if (!isCurrent(expectedGeneration)) return@bindSkills
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
        generation++
        active = true
        index.register(this)
        ArcartXAPI.getUIRegistry().open(viewer, "OrryxSkillHUD")
        update()
    }

    override fun close() {
        remove(true)
    }

    private fun deactivate(close: Boolean) {
        generation++
        active = false
        if (close) ArcartXAPI.getUIRegistry().close(viewer, "OrryxSkillHUD")
    }

    protected open fun remove(close: Boolean = true) {
        val indexed = index.getViewer(viewer.uniqueId)
        linkedSetOf<ArcartXSkillHud>().apply {
            indexed?.let(::add)
            add(this@ArcartXSkillHud)
        }.forEach { hud ->
            index.remove(hud)
            hud.deactivate(close)
        }
    }
}
