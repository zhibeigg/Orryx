package org.gitee.orryx.module.state

import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.StateParameter
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext

interface IActionState {

    /**
     * 状态键名
     * */
    val key: String

    /**
     * 脚本
     * */
    val script: Script?

    /**
     * 运行脚本
     * */
    fun runScript(playerData: PlayerData, function: (ScriptContext.() -> Unit)? = null) {
        script?.let {
            ScriptManager.runScript(adaptPlayer(playerData.player), StateParameter(playerData), it) {
                function?.invoke(this)
            }
        }
    }

}