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

}