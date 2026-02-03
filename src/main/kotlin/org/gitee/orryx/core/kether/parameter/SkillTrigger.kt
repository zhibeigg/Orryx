package org.gitee.orryx.core.kether.parameter

import org.gitee.orryx.core.key.IBindKey

/**
 * 技能触发方式密封类。
 *
 * 定义技能的不同触发来源。
 */
sealed class SkillTrigger {

    /**
     * 触发方式的标识名称。
     */
    abstract val name: String

    /**
     * 按键触发。
     *
     * @property bindKey 触发的按键绑定
     */
    data class Key(val bindKey: IBindKey) : SkillTrigger() {
        override val name: String = "KEY"
    }

    /**
     * 指令触发。
     *
     * @property command 触发的指令（可选）
     */
    data class Command(val command: String? = null) : SkillTrigger() {
        override val name: String = "COMMAND"
    }

    /**
     * API 触发。
     *
     * @property source 触发来源标识（可选）
     */
    data class Api(val source: String? = null) : SkillTrigger() {
        override val name: String = "API"
    }

    /**
     * 脚本触发（Kether 脚本内部调用）。
     */
    data object Script : SkillTrigger() {
        override val name: String = "SCRIPT"
    }

    /**
     * 未知触发方式。
     */
    data object Unknown : SkillTrigger() {
        override val name: String = "UNKNOWN"
    }
}
