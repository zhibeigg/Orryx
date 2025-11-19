package org.gitee.orryx.core.kts

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

/**
 * KTS 脚本执行上下文
 * 提供脚本执行时的变量存储和访问
 *
 * @property plugin 插件实例
 * @property player 执行脚本的玩家(可选)
 */
class KtsScriptContext(
    val plugin: Plugin,
    val player: Player? = null
) {

    /**
     * 变量存储容器
     */
    private val variables = ConcurrentHashMap<String, Any>()

    /**
     * 全局变量存储容器(跨上下文共享)
     */
    companion object {
        private val globalVariables = ConcurrentHashMap<String, Any>()

        /**
         * 设置全局变量
         */
        fun setGlobal(key: String, value: Any?) {
            if (value == null) {
                globalVariables.remove(key)
            } else {
                globalVariables[key] = value
            }
        }

        /**
         * 获取全局变量
         */
        fun getGlobal(key: String): Any? {
            return globalVariables[key]
        }

        /**
         * 移除全局变量
         */
        fun removeGlobal(key: String): Any? {
            return globalVariables.remove(key)
        }

        /**
         * 清空所有全局变量
         */
        fun clearGlobal() {
            globalVariables.clear()
        }
    }

    /**
     * 设置变量
     */
    operator fun set(key: String, value: Any?) {
        if (value == null) {
            variables.remove(key)
        } else {
            variables[key] = value
        }
    }

    /**
     * 获取变量
     */
    operator fun get(key: String): Any? {
        return variables[key]
    }

    /**
     * 获取变量(带默认值)
     */
    fun getOrDefault(key: String, default: Any?): Any? {
        return variables.getOrDefault(key, default)
    }

    /**
     * 移除变量
     */
    fun remove(key: String): Any? {
        return variables.remove(key)
    }

    /**
     * 清空所有变量
     */
    fun clear() {
        variables.clear()
    }

    /**
     * 批量设置变量
     */
    fun putAll(map: Map<String, Any>) {
        variables.putAll(map)
    }

    /**
     * 获取所有变量
     */
    fun getAll(): Map<String, Any> {
        return variables.toMap()
    }

    /**
     * 检查是否包含指定变量
     */
    fun contains(key: String): Boolean {
        return variables.containsKey(key)
    }

    /**
     * 创建子上下文(继承当前上下文的变量)
     */
    fun createChildContext(): KtsScriptContext {
        val child = KtsScriptContext(plugin, player)
        child.putAll(variables)
        return child
    }
}
