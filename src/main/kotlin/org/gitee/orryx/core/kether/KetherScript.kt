package org.gitee.orryx.core.kether

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

    override fun runActions(skillParameter: SkillParameter, map: Map<String, Any>?): CompletableFuture<Any?> {
        val future = CompletableFuture<Any?>()
        pluginScope.launch {
            debug("run skill: $skill action map: $map")
            val playerRunningSpace = ScriptManager.runningSkillScriptsMap.getOrPut(skillParameter.player.uniqueId) { PlayerRunningSpace(skillParameter.player) }

            var context: ScriptContext? = null
            ScriptManager.runScript(adaptPlayer(skillParameter.player), skillParameter, script) {
                playerRunningSpace.invoke(this, skill)
                map?.let { extend(it) }
                context = this
            }.whenComplete { v, ex ->
                playerRunningSpace.release(context!!, skill)
                if (ex != null) {
                    future.completeExceptionally(ex)
                } else {
                    future.complete(v)
                }
            }
        }
        return future
    }

    override fun runExtendActions(skillParameter: SkillParameter, extend: String, map: Map<String, Any>?): CompletableFuture<Any?> {
        val future = CompletableFuture<Any?>()
        pluginScope.launch {
            debug("run skill: $skill extend: $extend action")
            val playerRunningSpace = ScriptManager.runningSkillScriptsMap.getOrPut(skillParameter.player.uniqueId) { PlayerRunningSpace(skillParameter.player) }

            var context: ScriptContext? = null
            ScriptManager.runScript(adaptPlayer(skillParameter.player), skillParameter, script) {
                playerRunningSpace.invoke(this, "$skill@$extend")
                map?.let { extend(it) }
                context = this
            }.whenComplete { v, ex ->
                playerRunningSpace.release(context!!, "$skill@$extend")
                if (ex != null) {
                    future.completeExceptionally(ex)
                } else {
                    future.complete(v)
                }
            }
        }
        return future
    }
}