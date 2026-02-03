package org.gitee.orryx.core.kether

import org.gitee.orryx.core.kether.parameter.SkillParameter
import taboolib.module.kether.Script
import java.util.concurrent.CompletableFuture

/**
 * Kether 脚本接口。
 *
 * @property script 脚本实例
 */
interface IKetherScript {

    val script: Script

    /**
     * 运行脚本动作。
     *
     * @param skillParameter 技能参数
     * @param map 额外变量
     * @return 执行结果
     */
    fun runActions(skillParameter: SkillParameter, map: Map<String, Any?>? = null): CompletableFuture<Any?>

    /**
     * 运行扩展脚本动作。
     *
     * @param skillParameter 技能参数
     * @param extend 扩展脚本键名
     * @param map 额外变量
     * @return 执行结果
     */
    fun runExtendActions(skillParameter: SkillParameter, extend: String, map: Map<String, Any?>? = null): CompletableFuture<Any?>
}
