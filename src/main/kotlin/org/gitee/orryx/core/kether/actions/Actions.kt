package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.skill.*
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.OpenResult
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object Actions {

    init {
        KetherLoader.registerProperty(playerJobProperty(), IPlayerJob::class.java, false)
        KetherLoader.registerProperty(playerSkillProperty(), IPlayerSkill::class.java, false)
        KetherLoader.registerProperty(profileProperty(), IPlayerProfile::class.java, false)
        KetherLoader.registerProperty(skillProperty(), ISkill::class.java, false)
        KetherLoader.registerProperty(jobProperty(), IJob::class.java, false)
    }

    @KetherParser(["wait", "delay", "sleep"], namespace = ORRYX_NAMESPACE)
    private fun actionWait() = scriptParser(
        Action.new("普通语句", "延迟delay", "wait/delay/sleep")
            .description("延迟多少Tick")
            .addEntry("tick", Type.LONG)
    ) {
        val ticks = it.nextParsedAction()
        actionFuture { f ->
            run(ticks).long { ticks ->
                val task = submit(delay = ticks, async = !isPrimaryThread) {
                    f.complete(null)
                }
                addOrryxCloseable(f) { task.cancel() }
            }
        }
    }

    @KetherParser(["sync"], namespace = ORRYX_NAMESPACE)
    private fun actionSync() = scriptParser(
        Action.new("普通语句", "同步Sync", "sync")
            .description("将语句在主线程运行并等待返回")
            .addEntry("actions", Type.ANY)
    ) {
        val action = it.nextParsedAction()
        actionFuture { future ->
            ensureSync {
                run(action).thenAccept { value ->
                    future.complete(value)
                }
            }
        }
    }

    @KetherParser(["contains"], namespace = ORRYX_NAMESPACE)
    private fun actionContains() = scriptParser(
        Action.new("普通语句", "是否包含", "contains")
            .description("Iterable或者String是否包含value")
            .addEntry("Iterable或者String", Type.ANY)
            .addEntry("value", Type.ANY)
            .result("是否包含", Type.BOOLEAN)
    ) {
        val check = it.nextParsedAction()
        val value = it.nextParsedAction()
        actionFuture { future ->
            run(check).thenAccept { check ->
                run(value).thenAccept { value ->
                    future.complete(
                        when (check) {
                            is Iterable<*> -> check.contains(value)
                            is String -> check.contains(value.toString())
                            else -> false
                        }
                    )
                }
            }
        }
    }

    @KetherParser(["runExtend"], namespace = ORRYX_NAMESPACE)
    private fun callExtend() = scriptParser(
        Action.new("普通语句", "运行拓展子Action", "runExtend")
            .description("运行拓展子Action，继承母环境上下文，但是私有上下文，返回运行结果(只能在技能环境中使用)")
            .addEntry("拓展名", Type.STRING)
            .addEntry("私有原点", Type.TARGET, true, "@self", "origin")
    ) {
        val key = it.nextParsedAction()
        val origin = it.nextHeadActionOrNull(arrayOf("origin"))
        actionFuture { future ->
            val skillParameter = script().getParameterOrNull() as? SkillParameter ?: return@actionFuture future.complete(null)
            run(key).str { key ->
                containerOrSelf(origin) { container ->
                    val origin =  container.firstInstanceOrNull<ITargetLocation<*>>()
                    val extendParameter = SkillParameter(skillParameter, origin)

                    extendParameter.runSkillExtendAction(key, script().rootFrame().variables().toMap())?.whenComplete { value, ex ->
                        if (ex != null) {
                            future.completeExceptionally(ex)
                        } else {
                            future.complete(value)
                        }
                    } ?: future.complete(null)
                }
            }
        }
    }

    @KetherParser(["directCast"], namespace = ORRYX_NAMESPACE)
    private fun cast() = scriptParser(
        Action.new("普通语句", "强制释放技能", "directCast")
            .description("强制释放技能，蓄力类技能进入蓄力")
            .addEntry("技能名", Type.STRING)
            .addEntry("等级", Type.INT)
            .addEntry("是否消耗(蓝,冷却,沉默)", Type.BOOLEAN)
            .addContainerEntry("释放者", true, "@self")
    ) {
        val key = it.nextParsedAction()
        val level =  it.nextParsedAction()
        val consume =  it.nextParsedAction()
        val they = it.nextTheyContainerOrSelf()
        actionNow {
            run(key).str { key ->
                run(level).int { level ->
                    run(consume).bool { consume ->
                        containerOrSelf(they) { container ->
                            val skill = SkillLoaderManager.getSkillLoader(key) as ICastSkill
                            container.forEachInstance<PlayerTarget> { player ->
                                skill.castSkill(player.getSource(), SkillParameter(skill.key, player.getSource(), level), consume)
                            }
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["directRelease"], namespace = ORRYX_NAMESPACE)
    private fun release() = scriptParser(
        Action.new("普通语句", "强制释放蓄力技能", "directRelease")
            .description("强制释放蓄力技能，使蓄力类技能退出蓄力状态，并释放效果")
            .addEntry("技能名", Type.STRING)
            .addContainerEntry("释放者", true, "@self")
    ) {
        val key = it.nextParsedAction()
        val they = it.nextTheyContainerOrSelf()
        actionNow {
            run(key).str { key ->
                containerOrSelf(they) { container ->
                    container.forEachInstance<PlayerTarget> { player ->
                        val pressing = PressSkillManager.pressTaskMap[player.uniqueId] ?: return@forEachInstance
                        if (pressing.first == key) {
                            pressing.second.complete()
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["tryCast"], namespace = ORRYX_NAMESPACE)
    private fun tryCast() = scriptParser(
        Action.new("普通语句", "尝试释放技能", "tryCast")
            .description("尝试释放技能，通过条件检测才会释放")
            .addEntry("技能名", Type.STRING)
            .addContainerEntry("释放者", true, "@self")
    ) {
        val key = it.nextParsedAction()
        val they = it.nextTheyContainerOrSelf()
        actionFuture { f ->
            run(key).str { key ->
                containerOrSelf(they) { container ->
                    CompletableFuture.allOf(
                        *container.mapInstance<PlayerTarget, CompletableFuture<Void>> { player ->
                            player.getSource().getSkill(key).thenAccept { skill ->
                                skill?.tryCast()
                            }
                        }.toTypedArray()
                    ).thenRun {
                        f.complete(null)
                    }
                }
            }
        }
    }

    private fun playerJobProperty() = object : ScriptProperty<IPlayerJob>("orryx.player.job.operator") {

        override fun read(instance: IPlayerJob, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "name" -> OpenResult.successful(instance.job.name)
                "config" -> OpenResult.successful(instance.job)
                "player" -> OpenResult.successful(instance.player)
                "level" -> OpenResult.successful(instance.level)
                "maxLevel" -> OpenResult.successful(instance.maxLevel)
                "experienceOfLevel" -> OpenResult.successful(instance.experienceOfLevel)
                "maxExperienceOfLevel" -> OpenResult.successful(instance.maxExperienceOfLevel)
                "experience" -> OpenResult.successful(instance.experience)
                "binds" -> OpenResult.successful(instance.bindKeyOfGroup)
                "maxMana" -> OpenResult.successful(instance.getMaxMana())
                "regainMana" -> OpenResult.successful(instance.getRegainMana())
                "maxSpirit" -> OpenResult.successful(instance.getMaxSpirit())
                "regainSpirit" -> OpenResult.successful(instance.getRegainSpirit())
                "attributes" -> OpenResult.successful(instance.getAttributes())
                "spirit" -> OpenResult.successful(ISpiritManager.INSTANCE.getSpirit(instance.player))
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IPlayerJob, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    private fun playerSkillProperty() = object : ScriptProperty<IPlayerSkill>("orryx.player.skill.operator") {

        override fun read(instance: IPlayerSkill, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "job" -> OpenResult.successful(instance.job)
                "player" -> OpenResult.successful(instance.player)
                "level" -> OpenResult.successful(instance.level)
                "config" -> OpenResult.successful(instance.skill)
                "locked" -> OpenResult.successful(instance.locked)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IPlayerSkill, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    private fun profileProperty() = object : ScriptProperty<IPlayerProfile>("orryx.player.profile.operator") {

        override fun read(instance: IPlayerProfile, key: String): OpenResult {
            return when(key) {
                "point" -> OpenResult.successful(instance.point)
                "job" -> OpenResult.successful(instance.job)
                "player" -> OpenResult.successful(instance.player)
                "flags" -> OpenResult.successful(instance.flags)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IPlayerProfile, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    private fun skillProperty() = object : ScriptProperty<ISkill>("orryx.skill.operator") {

        override fun read(instance: ISkill, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "name" -> OpenResult.successful(instance.name)
                "type" -> OpenResult.successful(instance.type)
                "minLevel" -> OpenResult.successful(instance.minLevel)
                "maxLevel" -> OpenResult.successful(instance.maxLevel)
                "icon" -> OpenResult.successful(instance.icon)
                "locked" -> OpenResult.successful(instance.isLocked)
                "sort" -> OpenResult.successful(instance.sort)
                "material" -> OpenResult.successful(instance.xMaterial)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: ISkill, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    private fun jobProperty() = object : ScriptProperty<IJob>("orryx.player.profile.operator") {

        override fun read(instance: IJob, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "name" -> OpenResult.successful(instance.name)
                "experience" -> OpenResult.successful(instance.experience)
                "attributes" -> OpenResult.successful(instance.attributes)
                "skills" -> OpenResult.successful(instance.skills)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IJob, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}