package org.gitee.orryx.module.ui.arcartx

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.core.skill.SkillLevelResult
import org.gitee.orryx.module.ui.AbstractSkillUI
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.api.ArcartXAPI
import taboolib.common.function.debounce
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.warning
import java.io.File
import java.util.Collections
import java.util.concurrent.CompletableFuture

class ArcartXSkillUI(override val viewer: Player, override val owner: Player): AbstractSkillUI(viewer, owner) {

    private val debouncedUpdate = debounce(50L) {
        updateNow()
    }


    companion object {

        internal lateinit var skillUIConfiguration: YamlConfiguration

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is ArcartXUIManager) return
            skillUIConfiguration =
                YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/arcartx/OrryxSkillUI.yml"))
            ArcartXAPI.getUIRegistry().reload("OrryxSkillUI", skillUIConfiguration)
        }

        fun update(viewer: Player, owner: Player) {
            val uiRegistry = ArcartXAPI.getUIRegistry()
            owner.orryxProfile()
                .thenCompose { profile ->
                    owner.keySetting { it }.thenApply { profile to it }
                }
                .thenCompose { (profile, keySetting) ->
                    owner.job { it }.thenApply { Triple(profile, keySetting, it) }
                }
                .thenCompose { (profile, keySetting, job) ->
                    if (job == null) return@thenCompose CompletableFuture.completedFuture(null)
                    job.bindSkills { it }.thenCompose { bindSkills ->
                        job.skills { it }.thenCompose { skills ->
                            val pointMap = java.util.concurrent.ConcurrentHashMap<String, Int>()
                            CompletableFuture.allOf(
                                *skills.map { skill ->
                                    skill.upgradePointCheck(skill.level, skill.level + 1).thenAccept { pair ->
                                        pointMap[skill.key] = pair.first
                                    }
                                }.toTypedArray()
                            ).thenRun {
                                val keys = bindKeys()
                                uiRegistry.sendPacket(
                                    viewer, "OrryxSkillUI", "OrryxUIUpdate", mapOf(
                                    "Orryx_owner" to owner.uniqueId.toString(),
                                    "Orryx_job" to job.job.name,
                                    "Orryx_point" to profile.point,
                                    "Orryx_group" to job.group,
                                    "Orryx_bind_keys" to keys.map { key ->
                                        mapOf(
                                            "key" to key.key,
                                            "player_key" to (if (key.isClientKeyBind) key.defaultKey
                                                ?: "none" else keySetting.bindKeyMap[key] ?: "none"),
                                            "skill" to (bindSkills[key]?.key ?: "none"),
                                            "icon" to (bindSkills[key]?.getIcon() ?: "none")
                                        )
                                    },
                                    "Orryx_skills" to skills.map { skill ->
                                        mapOf(
                                            "key" to skill.key,
                                            "name" to skill.skill.name,
                                            "canBind" to (skill.skill is ICastSkill),
                                            "level" to skill.level,
                                            "maxLevel" to skill.skill.maxLevel,
                                            "locked" to skill.locked,
                                            "point" to (pointMap[skill.key] ?: 0)
                                        )
                                    }
                                ))
                            }
                        }
                    }
                }.exceptionally { ex ->
                    warning("[Orryx] OrryxUIUpdate error: ${ex.message}")
                    ex.printStackTrace()
                    null
                }
        }

        fun sendDescription(viewer: Player, owner: Player, skill: String) {
            owner.skill(skill) { playerSkill ->
                ArcartXAPI.getUIRegistry().sendPacket(
                    viewer, "OrryxSkillUI", "OrryxDescription", mapOf(
                        "skill" to skill,
                        "description" to playerSkill.getDescriptionComparison().joinToString("<n>")
                    )
                )
            }.exceptionally { ex ->
                warning("[Orryx] OrryxDescription error: ${ex.message}")
                null
            }
        }

        fun bindSkill(viewer: Player, owner: Player, group: String, bindKey: String, skill: String) {
            debug { "[ArcartX] bindSkill called: viewer=${viewer.name}, owner=${owner.name}, group=$group, bindKey=$bindKey, skill=$skill" }
            if (viewer == owner || viewer.isOp) {
                owner.job { job ->
                    val iGroup = BindKeyLoaderManager.getGroup(group)
                    val iBindKey = BindKeyLoaderManager.getBindKey(bindKey)
                    debug { "[ArcartX] bindSkill resolved: job=${job.key}, group=${iGroup?.key}, bindKey=${iBindKey?.key}" }
                    iGroup?.let { group ->
                        iBindKey?.let { bindKey ->
                            owner.skill(skill) { skill ->
                                debug { "[ArcartX] bindSkill setBindKey: skill=${skill.key}, group=${group.key}, bindKey=${bindKey.key}" }
                                job.setBindKey(skill, group, bindKey).thenRun {
                                    debug { "[ArcartX] bindSkill setBindKey success, updating UI" }
                                    update(viewer, owner)
                                }
                            }
                        } ?: debug { "[ArcartX] bindSkill failed: bindKey '$bindKey' not found" }
                    } ?: debug { "[ArcartX] bindSkill failed: group '$group' not found" }
                }
            } else {
                debug { "[ArcartX] bindSkill failed: permission denied" }
            }
        }

        fun unBindSkill(viewer: Player, owner: Player, group: String, skill: String) {
            debug { "[ArcartX] unBindSkill called: viewer=${viewer.name}, owner=${owner.name}, group=$group, skill=$skill" }
            if (viewer == owner || viewer.isOp) {
                owner.job { job ->
                    BindKeyLoaderManager.getGroup(group)?.let { group ->
                        owner.skill(skill) { skill ->
                            debug { "[ArcartX] unBindSkill: skill=${skill.key}, group=${group.key}" }
                            job.unBindKey(skill, group).thenRun {
                                debug { "[ArcartX] unBindSkill success, updating UI" }
                                update(viewer, owner)
                            }
                        }
                    } ?: debug { "[ArcartX] unBindSkill failed: group '$group' not found" }
                }
            }
        }

        fun upgrade(viewer: Player, owner: Player, skill: String) {
            if (viewer == owner || viewer.isOp) {
                owner.skill(skill) {
                    it.up().thenAccept { result ->
                        if (result == SkillLevelResult.SUCCESS) {
                            update(viewer, owner)
                            sendDescription(viewer, owner, skill)
                        }
                    }
                }
            }
        }

        fun downgrade(viewer: Player, owner: Player, skill: String) {
            if (viewer == owner || viewer.isOp) {
                owner.skill(skill) {
                    it.down().thenAccept { result ->
                        if (result == SkillLevelResult.SUCCESS) {
                            update(viewer, owner)
                            sendDescription(viewer, owner, skill)
                        }
                    }
                }
            }
        }

        fun clearAndBackPoint(viewer: Player, owner: Player, skill: String) {
            if (viewer == owner || viewer.isOp) {
                owner.skill(skill) {
                    it.clearLevelAndBackPoint().thenAccept { result ->
                        if (result) {
                            update(viewer, owner)
                            sendDescription(viewer, owner, skill)
                        }
                    }
                }
            }
        }
    }

    override fun open() {
        ArcartXAPI.getUIRegistry().open(viewer, "OrryxSkillUI")
        update()
    }

    override fun update() {
        debouncedUpdate()
    }

    private fun updateNow() {
        update(viewer, owner)
    }
}
