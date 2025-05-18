package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.container
import org.gitee.orryx.utils.containerOrSelf
import org.gitee.orryx.utils.ensureSync
import org.gitee.orryx.utils.firstInstanceOrNull
import org.gitee.orryx.utils.getParameterOrNull
import org.gitee.orryx.utils.nextHeadActionOrNull
import org.gitee.orryx.utils.readContainer
import org.gitee.orryx.utils.runSkillExtendAction
import org.gitee.orryx.utils.scriptParser
import taboolib.common.OpenResult
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.module.kether.*

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
            ensureSync { run(action).thenAccept { value -> future.complete(value) } }
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
            .description("运行拓展子Action，返回运行结果(只能在技能环境中使用)")
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
                    extendParameter.runSkillExtendAction(key)?.whenComplete { value, ex ->
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

    private fun playerJobProperty() = object : ScriptProperty<IPlayerJob>("orryx.player.job.operator") {

        override fun read(instance: IPlayerJob, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
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