package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object OrryxActions {

    @KetherParser(["orryx"], namespace = ORRYX_NAMESPACE)
    private fun actionOrryx() = ScriptManager.scriptParser(
        arrayOf(
            Action.new("Orryx信息获取", "获取玩家等级", "orryx", false)
                .description("获取玩家等级")
                .addEntry("获取玩家等级", Type.SYMBOL, false, head = "level")
                .addJob(optional = true, default = "当前职业")
                .result("等级", Type.INT),
            Action.new("Orryx信息获取", "获取技能点", "orryx", false)
                .description("获取玩家技能点")
                .addEntry("获取玩家技能点", Type.SYMBOL, false, head = "point")
                .result("技能点", Type.INT),
            Action.new("Orryx信息获取", "获取玩家所有经验", "orryx", false)
                .description("获取玩家所有经验")
                .addEntry("获取玩家所有经验", Type.SYMBOL, false, head = "experience/exp")
                .addJob(optional = true, default = "当前职业")
                .result("经验", Type.INT),
            Action.new("Orryx信息获取", "获取玩家当前等级经验", "orryx", false)
                .description("获取玩家当前等级经验")
                .addEntry("获取玩家当前等级经验", Type.SYMBOL, false, head = "experienceOfLevel/expOfLevel")
                .addJob(optional = true, default = "当前职业")
                .result("经验", Type.INT),
            Action.new("Orryx信息获取", "获取玩家当前等级最大经验", "orryx", false)
                .description("获取玩家当前等级最大经验")
                .addEntry("获取玩家当前等级最大经验", Type.SYMBOL, false, head = "maxExperienceOfLevel/maxExpOfLevel")
                .addJob(optional = true, default = "当前职业")
                .result("经验", Type.INT),
            Action.new("Orryx信息获取", "获取玩家技能组", "orryx", false)
                .description("获取玩家技能组")
                .addEntry("获取玩家技能组", Type.SYMBOL, false, head = "group")
                .addJob(optional = true, default = "当前职业")
                .result("技能组", Type.STRING),
            Action.new("Orryx信息获取", "获取玩家绑定技能", "orryx", false)
                .description("获取玩家绑定技能")
                .addEntry("获取玩家绑定技能", Type.SYMBOL, false, head = "bindSkill")
                .addJob(optional = true, default = "当前职业")
                .result("绑定技能", Type.STRING)
        )
    ) {
        it.switch {
            case("level") { level(it) }
            case("point") { point() }
            case("experience", "exp") { experience(it) }
            case("experienceOfLevel", "expOfLevel") { experienceOfLevel(it) }
            case("maxExperienceOfLevel", "maxExpOfLevel") { maxExperienceOfLevel(it) }
            case("group") { group(it) }
            case("bindSkill") { bindSkill(it) }
        }
    }

    private fun level(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadAction("job")
        return actionTake {
            skillCaster {
                if (job != null) {
                    run(job).str {
                        job(it).level
                    }
                } else {
                    CompletableFuture.completedFuture(job()?.level)
                }
            }
        }
    }

    private fun point(): ScriptAction<Any?> {
        return actionTake {
            skillCaster {
                CompletableFuture.completedFuture(orryxProfile().point)
            }
        }
    }

    private fun experience(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadAction("job")
        return actionTake {
            skillCaster {
                if (job != null) {
                    run(job).str {
                        job(it).experience
                    }
                } else {
                    CompletableFuture.completedFuture(job()?.experience)
                }
            }
        }
    }

    private fun experienceOfLevel(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadAction("job")
        return actionTake {
            skillCaster {
                if (job != null) {
                    run(job).str {
                        job(it).experienceOfLevel
                    }
                } else {
                    CompletableFuture.completedFuture(job()?.experienceOfLevel)
                }
            }
        }
    }

    private fun maxExperienceOfLevel(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadAction("job")
        return actionTake {
            skillCaster {
                if (job != null) {
                    run(job).str {
                        job(it).maxExperienceOfLevel
                    }
                } else {
                    CompletableFuture.completedFuture(job()?.maxExperienceOfLevel)
                }
            }
        }
    }

    private fun group(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadAction("job")
        return actionTake {
            skillCaster {
                if (job != null) {
                    run(job).str {
                        job(it).group
                    }
                } else {
                    CompletableFuture.completedFuture(job()?.group)
                }
            }
        }
    }

    private fun bindSkill(reader: QuestReader): ScriptAction<Any?> {
        val group = reader.nextParsedAction()
        val key = reader.nextParsedAction()
        return actionTake {
            run(group).str { group ->
                run(key).str { key ->
                    skillCaster {
                        CompletableFuture.completedFuture(getGroupSkills(group)[BindKeyLoaderManager.getBindKey(key)])
                    }
                }
            }
        }
    }

}