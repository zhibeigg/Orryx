package org.gitee.orryx.module.ui.dragoncore

import eos.moe.dragoncore.network.PacketSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.core.skill.SkillLevelResult
import org.gitee.orryx.module.ui.AbstractSkillUI
import org.gitee.orryx.module.ui.IUIManager
import org.gitee.orryx.utils.Pair
import org.gitee.orryx.utils.bindKeys
import org.gitee.orryx.utils.bindSkills
import org.gitee.orryx.utils.getDescriptionComparison
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.skill
import org.gitee.orryx.utils.skills
import org.gitee.orryx.utils.up
import taboolib.common.platform.function.getDataFolder
import taboolib.platform.util.onlinePlayers
import java.io.File
import java.util.concurrent.CompletableFuture

class DragonCoreSkillUI(override val viewer: Player, override val owner: Player): AbstractSkillUI(viewer, owner) {

    companion object {

        internal lateinit var skillUIConfiguration: YamlConfiguration

        @Reload(2)
        private fun reload() {
            if (IUIManager.INSTANCE !is DragonCoreUIManager) return
            skillUIConfiguration = YamlConfiguration.loadConfiguration(File(getDataFolder(), "ui/dragoncore/OrryxSkillUI.yml"))
            onlinePlayers.forEach {
                PacketSender.sendYaml(it, "gui/OrryxSkillUI.yml", skillUIConfiguration)
            }
        }

        fun update(viewer: Player, owner: Player) {
            owner.orryxProfile { profile ->
                owner.job { job ->
                    job.bindSkills { bindSkills ->
                        job.skills { skills ->
                            val list = mutableListOf<Pair<Int, Boolean>>()
                            CompletableFuture.allOf(
                                *skills.map { skill ->
                                    skill.upgradePointCheck(skill.level, skill.level+1).thenAccept { pair ->
                                        list += pair
                                    }
                                }.toTypedArray()
                            ).thenRun {
                                val keys = bindKeys()
                                PacketSender.sendSyncPlaceholder(viewer, mapOf(
                                    "Orryx_owner" to owner.uniqueId.toString(),
                                    "Orryx_job" to job.job.name,
                                    "Orryx_point" to profile.point.toString(),
                                    "Orryx_group" to job.group,
                                    "Orryx_bind_keys_ui" to keys.joinToString("<br>") { it.key },
                                    "Orryx_bind_skills_ui" to keys.joinToString("<br>") { bindSkills[it]?.key ?: "none" },
                                    "Orryx_skills" to skills.joinToString("<br>") { it.key },
                                    "Orryx_skills_name" to skills.joinToString("<br>") { it.skill.name },
                                    "Orryx_skills_canBind" to skills.joinToString("<br>") { (it.skill is ICastSkill).toString() },
                                    "Orryx_skills_level" to skills.joinToString("<br>") { it.level.toString() },
                                    "Orryx_skills_maxLevel" to skills.joinToString("<br>") { it.skill.maxLevel.toString() },
                                    "Orryx_skills_locked" to skills.joinToString("<br>") { it.locked.toString() },
                                    "Orryx_skills_point" to list.joinToString("<br>") { it.first.toString() }
                                ))
                            }
                        }
                    }
                }
            }
        }

        fun sendDescription(viewer: Player, owner: Player, skill: String) {
            owner.skill(skill) { playerSkill ->
                PacketSender.sendSyncPlaceholder(viewer, mapOf(
                    "Orryx_description" to playerSkill.getDescriptionComparison().joinToString("\n")
                ))
            }
        }

        fun bindSkill(viewer: Player, owner: Player, group: String, bindKey: String, skill: String) {
            if (viewer == owner || viewer.isOp) {
                owner.job { job ->
                    BindKeyLoaderManager.getGroup(group)?.let { group ->
                        BindKeyLoaderManager.getBindKey(bindKey)?.let { bindKey ->
                            owner.skill(skill) { skill ->
                                job.setBindKey(skill, group, bindKey).thenRun {
                                    update(viewer, owner)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun unBindSkill(viewer: Player, owner: Player, group: String, skill: String) {
            if (viewer == owner || viewer.isOp) {
                owner.job { job ->
                    BindKeyLoaderManager.getGroup(group)?.let { group ->
                        owner.skill(skill) { skill ->
                            job.unBindKey(skill, group).thenRun {
                                update(viewer, owner)
                            }
                        }
                    }
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
    }

    override fun open() {
        PacketSender.sendOpenGui(viewer, "OrryxSkillUI")
        update()
    }

    override fun update() {
        update(viewer, owner)
    }
}