package org.gitee.orryx.core.kether.actions.compat.mythicmobs

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object MythicMobsActions {

    @KetherParser(["mythicmobs", "mythic", "mob"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun dragonCore() = scriptParser(
        arrayOf(
            Action.new("MythicMobs附属语句", "嘲讽怪物", "mythicmobs", true)
                .description("嘲讽怪物")
                .addEntry("嘲讽标识符", Type.SYMBOL, false, head = "taunt")
                .addContainerEntry("被嘲讽的怪物", optional = false, head = null)
                .addContainerEntry("嘲讽者", true, "@self"),
            Action.new("MythicMobs附属语句", "添加仇恨值", "mythicmobs", true)
                .description("添加仇恨值")
                .addEntry("仇恨表标识符", Type.SYMBOL, false, head = "threat")
                .addEntry("添加标识符", Type.SYMBOL, false, head = "add")
                .addEntry("仇恨值", Type.DOUBLE, optional = false)
                .addContainerEntry("怪物", optional = false, head = null)
                .addContainerEntry("目标", true, "@self"),
            Action.new("MythicMobs附属语句", "减少仇恨值", "mythicmobs", true)
                .description("减少仇恨值")
                .addEntry("仇恨表标识符", Type.SYMBOL, false, head = "threat")
                .addEntry("减少标识符", Type.SYMBOL, false, head = "take")
                .addEntry("仇恨值", Type.DOUBLE, optional = false)
                .addContainerEntry("怪物", optional = false, head = null)
                .addContainerEntry("目标", true, "@self"),
            Action.new("MythicMobs附属语句", "设置仇恨值", "mythicmobs", true)
                .description("设置仇恨值")
                .addEntry("仇恨表标识符", Type.SYMBOL, false, head = "threat")
                .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
                .addEntry("仇恨值", Type.DOUBLE, optional = false)
                .addContainerEntry("怪物", optional = false, head = null)
                .addContainerEntry("目标", true, "@self"),
            Action.new("MythicMobs附属语句", "释放MM技能", "mythicmobs", true)
                .description("释放MythicMobs技能")
                .addEntry("释放标识符", Type.SYMBOL, false, head = "cast")
                .addEntry("技能名", Type.STRING, optional = false)
                .addEntry("技能强度", Type.FLOAT, optional = false)
                .addContainerEntry("怪物", true, "@self", "trigger")
                .addContainerEntry("目标", true, "@self"),
        )
    ) {
        it.switch {
            case("taunt") { taunt(it) }
            case("threat") {
                when(it.expects("add", "sub", "set", "+", "-", "=")) {
                    "add", "+" -> addThreat(it)
                    "take", "-" -> takeThreat(it)
                    "set", "=" -> setThreat(it)
                    else -> error("语句mythicmobs中threat书写错误")
                }
            }
            case("signal") { signal(it) }
            case("cast") { castSkill(it) }
        }
    }

    private fun taunt(reader: QuestReader): ScriptAction<Any?> {
        val mobs = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            container(mobs) {mobs ->
                containerOrSelf(they) {they ->
                    val players = they.get<PlayerTarget>()
                    mobs.forEachInstance<ITargetEntity<Entity>> { target ->
                        players.forEach {
                            MythicMobs.inst().apiHelper.taunt(target.getSource(), it.getSource())
                        }
                    }
                }
            }
        }
    }

    private fun addThreat(reader: QuestReader): ScriptAction<Any?> {
        val amount = reader.nextParsedAction()
        val mobs = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(amount).double { amount ->
                container(mobs) {mobs ->
                    containerOrSelf(they) {they ->
                        val players = they.get<PlayerTarget>()
                        mobs.forEachInstance<ITargetEntity<Entity>> { target ->
                            players.forEach {
                                MythicMobs.inst().apiHelper.addThreat(target.getSource(), it.getSource(), amount)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun takeThreat(reader: QuestReader): ScriptAction<Any?> {
        val amount = reader.nextParsedAction()
        val mobs = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(amount).double { amount ->
                container(mobs) {mobs ->
                    containerOrSelf(they) {they ->
                        val players = they.get<PlayerTarget>()
                        mobs.forEachInstance<ITargetEntity<Entity>> { target ->
                            players.forEach {
                                MythicMobs.inst().apiHelper.reduceThreat(target.getSource(), it.getSource(), amount)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setThreat(reader: QuestReader): ScriptAction<Any?> {
        val amount = reader.nextParsedAction()
        val mobs = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(amount).double { amount ->
                container(mobs) {mobs ->
                    containerOrSelf(they) {they ->
                        val players = they.get<PlayerTarget>()
                        mobs.forEachInstance<ITargetEntity<Entity>> { target ->
                            players.forEach {
                                setThreat(target.getSource(), it.getSource(), amount)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setThreat(mob: Entity, target: LivingEntity, amount: Double): Boolean {
        if (!MythicMobs.inst().mobManager.isActiveMob(mob.uniqueId)) {
            return false
        } else {
            val am = MythicMobs.inst().mobManager.getMythicMobInstance(mob)
            if (am.threatTable == null) {
                return false
            } else {
                am.threatTable.threatSet(BukkitAdapter.adapt(target), amount)
                return true
            }
        }
    }

    private fun signal(reader: QuestReader): ScriptAction<Any?> {
        val signal = reader.nextParsedAction()
        val mobs = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(signal).str { signal ->
                container(mobs) {mobs ->
                    containerOrSelf(they) {they ->
                        val players = they.get<PlayerTarget>()
                        mobs.forEachInstance<ITargetEntity<Entity>> { target ->
                            players.forEach {
                                signal(target.getSource(), it.getSource(), signal)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun signal(mob: Entity, trigger: LivingEntity, signal: String): Boolean {
        if (!MythicMobs.inst().mobManager.isActiveMob(mob.uniqueId)) {
            return false
        } else {
            val am = MythicMobs.inst().mobManager.getMythicMobInstance(mob)
            am.signalMob(BukkitAdapter.adapt(trigger), signal)
            return true
        }
    }

    private fun castSkill(reader: QuestReader): ScriptAction<Any?> {
        val skillName = reader.nextParsedAction()
        val power = reader.nextParsedAction()
        val trigger = reader.nextHeadActionOrNull(arrayOf("trigger"))
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(skillName).str { skillName ->
                run(power).float { power ->
                    containerOrSelf(trigger) { mobs ->
                        containerOrSelf(they) { they ->
                            val caster = they.get<ITargetEntity<Entity>>()
                            mobs.forEachInstance<ITargetEntity<Entity>> { target ->
                                caster.forEach {
                                    MythicMobs.inst().apiHelper.castSkill(
                                        it.getSource(),
                                        skillName,
                                        target.getSource(),
                                        script().getParameterOrNull()?.origin?.location,
                                        null,
                                        null,
                                        power
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}