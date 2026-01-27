package org.gitee.orryx.module.state

import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.StateParameter
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext

/**
 * 动作状态接口。
 *
 * @property key 状态键名
 * @property script 状态脚本
 */
interface IActionState {

    val key: String

    val script: Script?

    /**
     * 运行脚本。
     *
     * @param playerData 状态运行上下文
     * @param function 脚本执行时的扩展处理
     */
    fun runScript(playerData: PlayerData, function: (ScriptContext.() -> Unit)? = null) {
        script?.let {
            ScriptManager.runScript(adaptPlayer(playerData.player), StateParameter(playerData), it) {
                function?.invoke(this)
            }
        }
    }
}
