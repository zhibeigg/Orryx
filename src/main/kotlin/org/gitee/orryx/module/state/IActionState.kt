package org.gitee.orryx.module.state

import taboolib.module.kether.Script

interface IActionState {

    /**
     * 状态键名
     * */
    val key: String

    /**
     * 运行脚本
     * */
    val script: Script?

    /**
     * 是否能过渡到下一个状态
     * @param input 用于读取下一操作的输入键
     * @return 是否能
     * */
    fun hasNext(input: String): Boolean

}