package org.gitee.orryx.core.kether.parameter

import org.bukkit.Location
import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.ticksToMillisSaturated
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.warning
import taboolib.common5.cdouble
import taboolib.common5.clong
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap

class SkillParameter(val skill: String?, val player: Player, level: Int = 1): IParameter {

    constructor(skillParameter: SkillParameter, origin: ITargetLocation<*>?): this(skillParameter.skill, skillParameter.player, skillParameter.level) {
        this.origin = origin ?: player.toTarget()
        this.trigger = skillParameter.trigger
    }

    var level: Int = level
        set(value) {
            if (field == value) return
            field = value
            lazies.clear()
            lazyFutures.values.forEach { it.cancel(false) }
            lazyFutures.clear()
        }

    override var origin: ITargetLocation<*>? = player.toTarget()

    /**
     * 技能触发方式。
     */
    var trigger: SkillTrigger = SkillTrigger.Unknown

    /**
     * 获取触发的按键绑定（如果是按键触发）。
     *
     * @return 按键绑定，如果不是按键触发则返回 null
     */
    fun getBindKey(): IBindKey? {
        return (trigger as? SkillTrigger.Key)?.bindKey
    }

    /**
     * 构建触发方式相关的变量 Map。
     *
     * @return 包含触发方式信息的变量 Map
     */
    fun buildTriggerVariables(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "triggerType" to trigger.name
        )
        when (val t = trigger) {
            is SkillTrigger.Key -> {
                map["bindKey"] = t.bindKey
                map["bindKeyName"] = t.bindKey.key
            }
            is SkillTrigger.Command -> {
                map["triggerCommand"] = t.command
            }
            is SkillTrigger.Api -> {
                map["triggerSource"] = t.source
            }
            else -> {}
        }
        return map
    }

    private val proxyCommandSender by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { adaptPlayer(player) }

    private val lazies by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<String, Any>() }
    private val lazyFutures by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ConcurrentHashMap<String, CompletableFuture<Any?>>()
    }

    fun getSkill(): ISkill? {
        return skill?.let { SkillLoaderManager.getSkillLoader(skill) }
    }

    fun getOriginLocation(): Location {
        return origin?.location ?: player.location
    }

    fun getVariableFuture(key: String, lazy: Boolean): CompletableFuture<Any?> {
        lazies[key]?.let { return CompletableFuture.completedFuture(it) }

        fun load(): CompletableFuture<Any?> {
            val action = getSkill()?.variables?.get(key)
            if (action == null) {
                warning("未找到技能 $skill 的变量 $key ")
                return CompletableFuture.completedFuture(null)
            }
            return ScriptManager.runScript(proxyCommandSender, this, action) {
                set("level", level)
            }.thenApply { value ->
                if (value == null) {
                    warning("未找到技能 $skill 的变量 $key ")
                } else {
                    lazies[key] = value
                }
                value
            }
        }

        if (!lazy) return load()
        val future = lazyFutures.computeIfAbsent(key) { load() }
        future.whenComplete { _, throwable ->
            if (throwable != null) lazyFutures.remove(key, future)
        }
        return future
    }

    override fun getVariable(key: String, lazy: Boolean): Any? {
        val future = getVariableFuture(key, lazy)
        check(future.isDone) { "技能变量 $key 包含异步动作，请使用 getVariableFuture" }
        return try {
            future.getNow(null)
        } catch (throwable: CompletionException) {
            throw throwable.cause ?: throwable
        }
    }

    override fun getVariable(key: String, default: Any): Any {
        return lazies[key] ?: lazies.putIfAbsent(key, default) ?: default
    }

    fun manaValue(lazy: Boolean = false): Double {
        return getVariable("MANA", lazy).cdouble
    }

    fun manaValueFuture(lazy: Boolean = false): CompletableFuture<Double> {
        return getVariableFuture("MANA", lazy).thenApply { it.cdouble }
    }

    fun cooldownValue(lazy: Boolean = false): Long {
        return ticksToMillisSaturated(getVariable("COOLDOWN", lazy).clong)
    }

    fun cooldownValueFuture(lazy: Boolean = false): CompletableFuture<Long> {
        return getVariableFuture("COOLDOWN", lazy).thenApply { ticksToMillisSaturated(it.clong) }
    }

    fun silenceValue(lazy: Boolean = false): Long {
        return ticksToMillisSaturated(getVariable("SILENCE", lazy).clong)
    }

    fun silenceValueFuture(lazy: Boolean = false): CompletableFuture<Long> {
        return getVariableFuture("SILENCE", lazy).thenApply { ticksToMillisSaturated(it.clong) }
    }

    override fun toString(): String {
        return "SkillParameter{skill=$skill, player=${player.name}, level=$level, trigger=${trigger.name}}"
    }
}