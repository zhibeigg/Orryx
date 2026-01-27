package org.gitee.orryx.core.kether.parameter

import org.gitee.orryx.core.targets.ITargetLocation

/**
 * 脚本参数接口。
 *
 * @property origin 参数来源目标
 */
interface IParameter {

    var origin: ITargetLocation<*>?

    /**
     * 获取变量。
     *
     * @param key 变量键名
     * @param lazy 是否惰性获取
     * @return 变量值，可能为 null
     */
    fun getVariable(key: String, lazy: Boolean): Any?

    /**
     * 获取变量，若不存在则返回默认值。
     *
     * @param key 变量键名
     * @param default 默认值
     * @return 变量值
     */
    fun getVariable(key: String, default: Any): Any

}
