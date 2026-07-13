package org.gitee.orryx.core.kether

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.gitee.orryx.api.OrryxAPI.Companion.pluginScope
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.debug
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.extend
import java.util.concurrent.CompletableFuture

/**
 * @author zhibei
 * @param skill 技能名
 * @throws IllegalStateException KetherScript初始化时检测到不存在的技能
 * @suppress 使用未注册的skill名
 * */
class KetherScript(val skill: String, override val script: Script): IKetherScript {

    init {
        if (SkillLoaderManager.getSkillLoader(skill) == null) error("KetherScript初始化时检测到不存在的技能$skill")
    }

    override fun runActions(skillParameter: SkillParameter, map: Map<String, Any?>?): CompletableFuture<Any?> {
        return launchActions(skill, script, skillParameter, map, started = null)
    }

    /**
     * 仅等待脚本上下文成功创建并提交执行，不等待整段技能脚本结束。
     * 用于把“脚本启动”纳入施法事务，避免长脚本阻塞玩家后续施法。
     */
    internal fun startActions(skillParameter: SkillParameter, map: Map<String, Any?>?): CompletableFuture<Unit> {
        val started = CompletableFuture<Unit>()
        launchActions(skill, script, skillParameter, map, started)
        return started
    }

    override fun runExtendActions(skillParameter: SkillParameter, extend: String, map: Map<String, Any?>?): CompletableFuture<Any?> {
        val scriptKey = "$skill@$extend"
        val extendScript = SkillLoaderManager.getSkillLoader(skill)?.let { loader ->
            (loader as? org.gitee.orryx.core.skill.ICastSkill)?.extendScripts?.get(extend)
        } ?: return CompletableFuture<Any?>().also {
            it.completeExceptionally(IllegalStateException("请修复技能配置中的错误$skill extend $extend"))
        }
        return launchActions(scriptKey, extendScript, skillParameter, map, started = null)
    }

    private fun launchActions(
        scriptKey: String,
        targetScript: Script,
        skillParameter: SkillParameter,
        map: Map<String, Any?>?,
        started: CompletableFuture<Unit>?,
    ): CompletableFuture<Any?> {
        val result = CompletableFuture<Any?>()
        val job = pluginScope.launch {
            try {
                debug { "run skill: $scriptKey action map: $map" }
                val playerRunningSpace = ScriptManager.runningSkillScriptsMap.getOrPut(skillParameter.player.uniqueId) {
                    PlayerRunningSpace(skillParameter.player)
                }
                var context: ScriptContext? = null
                val execution = ScriptManager.runScript(adaptPlayer(skillParameter.player), skillParameter, targetScript) {
                    playerRunningSpace.invoke(this, scriptKey)
                    map?.let { extend(it) }
                    context = this
                }
                execution.whenComplete { value, throwable ->
                    context?.let { playerRunningSpace.release(it, scriptKey) }
                    if (throwable == null) {
                        result.complete(value)
                    } else {
                        started?.completeExceptionally(throwable)
                        result.completeExceptionally(throwable)
                        if (started != null) throwable.printStackTrace()
                    }
                }
                started?.complete(Unit)
            } catch (throwable: Throwable) {
                started?.completeExceptionally(throwable)
                result.completeExceptionally(throwable)
            }
        }
        job.invokeOnCompletion { throwable ->
            if (throwable != null) {
                val failure = if (throwable is CancellationException) {
                    CancellationException("Orryx 技能脚本启动已取消").also { it.initCause(throwable) }
                } else {
                    throwable
                }
                started?.completeExceptionally(failure)
                result.completeExceptionally(failure)
            }
        }
        return result
    }
}
