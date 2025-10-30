package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common5.clong
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object OrryxActions {

    @KetherParser(["orryx"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionOrryx() = scriptParser(
        Action.new("Orryx信息获取", "获取玩家职业", "orryx", true)
            .description("获取玩家职业")
            .addEntry("获取玩家职业", Type.SYMBOL, false, head = "job")
            .result("职业", Type.STRING),
        Action.new("Orryx信息获取", "获取玩家职业实例", "orryx", true)
            .description("获取玩家职业实例")
            .addEntry("获取玩家职业", Type.SYMBOL, false, head = "jobInstance")
            .result("职业实例", Type.ANY),
        Action.new("Orryx信息获取", "获取玩家等级", "orryx", true)
            .description("获取玩家等级")
            .addEntry("获取玩家等级", Type.SYMBOL, false, head = "level")
            .addJob(optional = true, default = "当前职业")
            .result("等级", Type.INT),
        Action.new("Orryx信息获取", "获取技能点", "orryx", true)
            .description("获取玩家技能点")
            .addEntry("获取玩家技能点", Type.SYMBOL, false, head = "point")
            .result("技能点", Type.INT),
        Action.new("Orryx信息获取", "获取玩家所有经验", "orryx", true)
            .description("获取玩家所有经验")
            .addEntry("获取玩家所有经验", Type.SYMBOL, false, head = "experience/exp")
            .addJob(optional = true, default = "当前职业")
            .result("经验", Type.INT),
        Action.new("Orryx信息获取", "获取玩家当前等级经验", "orryx", true)
            .description("获取玩家当前等级经验")
            .addEntry("获取玩家当前等级经验", Type.SYMBOL, false, head = "experienceOfLevel/expOfLevel")
            .addJob(optional = true, default = "当前职业")
            .result("经验", Type.INT),
        Action.new("Orryx信息获取", "获取玩家当前等级最大经验", "orryx", true)
            .description("获取玩家当前等级最大经验")
            .addEntry("获取玩家当前等级最大经验", Type.SYMBOL, false, head = "maxExperienceOfLevel/maxExpOfLevel")
            .addJob(optional = true, default = "当前职业")
            .result("经验", Type.INT),
        Action.new("Orryx信息获取", "获取玩家技能组", "orryx", true)
            .description("获取玩家技能组")
            .addEntry("获取玩家技能组", Type.SYMBOL, false, head = "group")
            .addJob(optional = true, default = "当前职业")
            .result("技能组", Type.STRING),
        Action.new("Orryx信息获取", "获取玩家绑定技能", "orryx", true)
            .description("获取玩家绑定技能")
            .addEntry("获取玩家绑定技能", Type.SYMBOL, false, head = "bindSkill")
            .addJob(optional = true, default = "当前职业")
            .result("绑定技能", Type.STRING),
        Action.new("Orryx信息获取", "获取玩家技能等级", "orryx", true)
            .description("获取玩家技能等级")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("等级标识符", Type.SYMBOL, false, head = "level")
            .addEntry("技能", Type.STRING, false)
            .addJob(optional = true, default = "当前职业")
            .result("技能等级", Type.INT),
        Action.new("Orryx信息获取", "获取玩家技能是否锁定", "orryx", true)
            .description("获取玩家技能是否锁定")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("等级标识符", Type.SYMBOL, false, head = "locked")
            .addEntry("技能", Type.STRING, false)
            .addJob(optional = true, default = "当前职业")
            .result("技能是否锁定", Type.BOOLEAN),
        Action.new("Orryx信息获取", "获取技能最低等级", "orryx", true)
            .description("获取技能最低等级")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("最低等级标识符", Type.SYMBOL, false, head = "minLevel/min")
            .addEntry("技能", Type.STRING, false)
            .result("技能最低等级", Type.INT),
        Action.new("Orryx信息获取", "获取技能最高等级", "orryx", true)
            .description("获取技能最高等级")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("最高等级标识符", Type.SYMBOL, false, head = "maxLevel/max")
            .addEntry("技能", Type.STRING, false)
            .result("技能最高等级", Type.INT),
        Action.new("Orryx信息获取", "获取玩家技能冷却", "orryx", true)
            .description("获取玩家技能冷却")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("冷却标识符", Type.SYMBOL, false, head = "cooldown")
            .addEntry("技能", Type.STRING, false)
            .addJob(optional = true, default = "当前职业")
            .result("技能冷却", Type.LONG),
        Action.new("Orryx信息获取", "获取玩家技能冷却倒计时", "orryx", true)
            .description("获取玩家技能冷却倒计时")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("倒计时标识符", Type.SYMBOL, false, head = "countdown")
            .addEntry("技能", Type.STRING, false)
            .addJob(optional = true, default = "当前职业")
            .result("技能冷却倒计时", Type.LONG),
        Action.new("Orryx信息获取", "获取玩家技能消耗法力值", "orryx", true)
            .description("获取玩家技能消耗法力值")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("法力值标识符", Type.SYMBOL, false, head = "mana")
            .addEntry("技能", Type.STRING, false)
            .addJob(optional = true, default = "当前职业")
            .result("技能释放消耗法力值", Type.DOUBLE),
        Action.new("Orryx信息获取", "获取玩家技能沉默时间", "orryx", true)
            .description("获取玩家技能沉默时间")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("沉默标识符", Type.SYMBOL, false, head = "silence")
            .addEntry("技能", Type.STRING, false)
            .addJob(optional = true, default = "当前职业")
            .result("技能释放沉默时间", Type.DOUBLE),
        Action.new("Orryx信息获取", "获取玩家技能前置变量", "orryx", true)
            .description("获取玩家技能前置变量")
            .addEntry("技能标识符", Type.SYMBOL, false, head = "skill")
            .addEntry("变量标识符", Type.SYMBOL, false, head = "variables/var")
            .addEntry("技能", Type.STRING, false)
            .addEntry("变量名", Type.STRING, false)
            .addJob(optional = true, default = "当前职业")
            .result("技能释放沉默时间", Type.DOUBLE)
    ) {
        it.switch {
            case("job") { job() }
            case("jobInstance") { jobInstance(it) }
            case("level") { level(it) }
            case("point") { point() }
            case("experience", "exp") { experience(it) }
            case("experienceOfLevel", "expOfLevel") { experienceOfLevel(it) }
            case("maxExperienceOfLevel", "maxExpOfLevel") { maxExperienceOfLevel(it) }
            case("group") { group(it) }
            case("bindSkill") { bindSkill(it) }
            case("skill") {
                it.switch {
                    case("level") { skillLevel(it) }
                    case("locked") { skillLocked(it) }
                    case("minLevel", "min") { skillMinLevel(it) }
                    case("maxLevel", "max") { skillMaxLevel(it) }
                    case("cooldown") { skillCooldown(it) }
                    case("countdown") { skillCountdown(it) }
                    case("mana") { skillMana(it) }
                    case("silence") { skillSilence(it) }
                    case("variables", "var") { skillVariables(it) }
                    other { error("kether orryx skill书写错误") }
                }
            }
        }
    }

    private fun job(): ScriptAction<Any?> {
        return actionFuture { future ->
            skillCaster {
                orryxProfileTo {
                    future.complete(it.job)
                }
            }
        }
    }

    private fun jobInstance(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadActionOrNull("job")
        return actionFuture { future ->
            skillCaster {
                if (job != null) {
                    run(job).str {
                        orryxProfileTo { profile ->
                            job(profile.id, it) { job ->
                                future.complete(job)
                            }
                        }
                    }
                } else {
                    job { it -> future.complete(it) }
                }
            }
        }
    }

    private fun level(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadActionOrNull("job")
        return actionFuture { future ->
            skillCaster {
                if (job != null) {
                    run(job).str {
                        orryxProfileTo { profile ->
                            job(profile.id, it) { job ->
                                future.complete(job.level)
                            }
                        }
                    }
                } else {
                    job { it -> future.complete(it.level) }
                }
            }
        }
    }

    private fun point(): ScriptAction<Any?> {
        return actionFuture { future ->
            skillCaster {
                orryxProfileTo {
                    future.complete(it.point)
                }
            }
        }
    }

    private fun experience(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadActionOrNull("job")
        return actionFuture { future ->
            skillCaster {
                if (job != null) {
                    run(job).str {
                        orryxProfileTo { profile ->
                            job(profile.id, it) { job ->
                                future.complete(job.experience)
                            }
                        }
                    }
                } else {
                    job { it -> future.complete(it.experience) }
                }
            }
        }
    }

    private fun experienceOfLevel(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadActionOrNull("job")
        return actionFuture { future ->
            skillCaster {
                if (job != null) {
                    run(job).str {
                        orryxProfileTo { profile ->
                            job(profile.id, it) { job ->
                                future.complete(job.experienceOfLevel)
                            }
                        }
                    }
                } else {
                    job { it -> future.complete(it.experienceOfLevel) }
                }
            }
        }
    }

    private fun maxExperienceOfLevel(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadActionOrNull("job")
        return actionFuture { future ->
            skillCaster {
                if (job != null) {
                    run(job).str {
                        orryxProfileTo { profile ->
                            job(profile.id, it) { job ->
                                future.complete(job.maxExperienceOfLevel)
                            }
                        }
                    }
                } else {
                    job { it -> future.complete(it.maxExperienceOfLevel) }
                }
            }
        }
    }

    private fun group(reader: QuestReader): ScriptAction<Any?> {
        val job = reader.nextHeadActionOrNull("job")
        return actionFuture { future ->
            skillCaster {
                if (job != null) {
                    run(job).str {
                        orryxProfileTo { profile ->
                            job(profile.id, it) { job ->
                                future.complete(job.group)
                            }
                        }
                    }
                } else {
                    job { it -> future.complete(it.group) }
                }
            }
        }
    }

    private fun bindSkill(reader: QuestReader): ScriptAction<Any?> {
        val group = reader.nextParsedAction()
        val key = reader.nextParsedAction()
        return actionFuture { future ->
            run(group).str { group ->
                run(key).str { key ->
                    skillCaster {
                        getGroupSkills(group).thenApply {
                            it?.get(BindKeyLoaderManager.getBindKey(key))?.apply {
                                future.complete(this)
                            } ?: kotlin.run {
                                future.complete(null)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun skillLevel(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()
        val job = reader.nextHeadActionOrNull("job")

        return actionFuture { future ->
            skillCaster {
                run(skill).str { skill ->
                    if (job != null) {
                        run(job).str { job ->
                            getSkill(job, skill, true).thenApply {
                                future.complete(it?.level)
                            }
                        }
                    } else {
                        getSkill(skill, true).thenApply { it ->
                            future.complete(it?.level)
                        }
                    }
                }
            }
        }
    }

    private fun skillLocked(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()
        val job = reader.nextHeadActionOrNull("job")

        return actionFuture { future ->
            skillCaster {
                run(skill).str { skill ->
                    if (job != null) {
                        run(job).str { job ->
                            getSkill(job, skill, true).thenApply {
                                future.complete(it?.locked)
                            }
                        }
                    } else {
                        getSkill(skill, true).thenApply { it ->
                            future.complete(it?.locked)
                        }
                    }
                }
            }
        }
    }

    private fun skillMinLevel(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()

        return actionFuture { future ->
            run(skill).str { skill ->
                future.complete(SkillLoaderManager.getSkillLoader(skill)?.minLevel)
            }
        }
    }

    private fun skillMaxLevel(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()

        return actionFuture { future ->
            run(skill).str { skill ->
                future.complete(SkillLoaderManager.getSkillLoader(skill)?.maxLevel)
            }
        }
    }

    private fun skillCooldown(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()
        val job = reader.nextHeadActionOrNull("job")

        return actionFuture { future ->
            skillCaster {
                run(skill).str { skill ->
                    if (job != null) {
                        run(job).str { job ->
                            getSkill(job, skill, true).thenApply {
                                val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                                future.complete(skillParameter?.getVariable("COOLDOWN", true).clong)
                            }
                        }
                    } else {
                        getSkill(skill, true).thenApply { it ->
                            val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                            future.complete(skillParameter?.getVariable("COOLDOWN", true).clong)
                        }
                    }
                }
            }
        }
    }

    private fun skillCountdown(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()
        val job = reader.nextHeadActionOrNull("job")

        return actionFuture { future ->
            skillCaster {
                run(skill).str { skill ->
                    if (job != null) {
                        run(job).str { job ->
                            getSkill(job, skill, true).thenApply {
                                future.complete(SkillTimer.getCountdown(this, skill))
                            }
                        }
                    } else {
                        getSkill(skill, true).thenApply {
                            future.complete(SkillTimer.getCountdown(this, skill))
                        }
                    }
                }
            }
        }
    }

    private fun skillMana(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()
        val job = reader.nextHeadActionOrNull("job")

        return actionFuture { future ->
            skillCaster {
                run(skill).str { skill ->
                    if (job != null) {
                        run(job).str { job ->
                            getSkill(job, skill, true).thenApply {
                                val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                                future.complete(skillParameter?.manaValue(true) ?: 0)
                            }
                        }
                    } else {
                        getSkill(skill, true).thenApply { it ->
                            val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                            future.complete(skillParameter?.manaValue(true) ?: 0)
                        }
                    }
                }
            }
        }
    }

    private fun skillSilence(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()
        val job = reader.nextHeadActionOrNull("job")

        return actionFuture { future ->
            skillCaster {
                run(skill).str { skill ->
                    if (job != null) {
                        run(job).str { job ->
                            getSkill(job, skill, true).thenApply {
                                val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                                future.complete(skillParameter?.getVariable("SILENCE", true).clong)
                            }
                        }
                    } else {
                        getSkill(skill, true).thenApply { it ->
                            val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                            future.complete(skillParameter?.getVariable("SILENCE", true).clong)
                        }
                    }
                }
            }
        }
    }

    private fun skillVariables(reader: QuestReader): ScriptAction<Any?> {
        val skill = reader.nextParsedAction()
        val variable = reader.nextParsedAction()
        val job = reader.nextHeadActionOrNull("job")

        return actionFuture { future ->
            skillCaster {
                run(skill).str { skill ->
                    run(variable).str { variable ->
                        if (job != null) {
                            run(job).str { job ->
                                getSkill(job, skill, true).thenApply {
                                    val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                                    future.complete(skillParameter?.getVariable(variable, false))
                                }
                            }
                        } else {
                            getSkill(skill, true).thenApply { it ->
                                val skillParameter = it?.level?.let { level -> SkillParameter(skill, this, level) }
                                future.complete(skillParameter?.getVariable(variable, false))
                            }
                        }
                    }
                }
            }
        }
    }
}