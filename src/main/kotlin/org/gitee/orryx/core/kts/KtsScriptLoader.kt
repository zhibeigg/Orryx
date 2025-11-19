package org.gitee.orryx.core.kts

import taboolib.module.configuration.Configuration
import java.io.File

/**
 * KTS 脚本接口
 * 用于表示一个可执行的脚本
 */
interface IKtsScript {

    /**
     * 脚本唯一标识
     */
    val key: String

    /**
     * 脚本显示名称
     */
    val name: String

    /**
     * 脚本描述
     */
    val description: String

    /**
     * 脚本文件
     */
    val file: File

    /**
     * 脚本作者
     */
    val author: String

    /**
     * 脚本版本
     */
    val version: String

    /**
     * 是否启用
     */
    val enabled: Boolean

    /**
     * 执行超时时间(毫秒)
     */
    val timeout: Long

    /**
     * 脚本依赖的其他脚本
     */
    val dependencies: List<String>
}

/**
 * KTS 脚本加载器
 * 从配置文件加载脚本信息
 */
class KtsScriptLoader(
    override val key: String,
    private val configuration: Configuration,
    private val scriptFile: File
) : IKtsScript {

    private val options by lazy {
        configuration.getConfigurationSection("Options")
            ?: error("脚本 $key 位于 ${configuration.file} 未书写 Options 键")
    }

    override val name: String by lazy {
        options.getString("Name", key) ?: key
    }

    override val description: String by lazy {
        options.getString("Description", "无描述") ?: "无描述"
    }

    override val file: File = scriptFile

    override val author: String by lazy {
        options.getString("Author", "Unknown") ?: "Unknown"
    }

    override val version: String by lazy {
        options.getString("Version", "1.0.0") ?: "1.0.0"
    }

    override val enabled: Boolean by lazy {
        options.getBoolean("Enabled", true)
    }

    override val timeout: Long by lazy {
        options.getLong("Timeout", 0L)
    }

    override val dependencies: List<String> by lazy {
        options.getStringList("Dependencies")
    }

    /**
     * 获取脚本初始变量
     */
    fun getVariables(): Map<String, Any?> {
        val variablesSection = options.getConfigurationSection("Variables") ?: return emptyMap()
        return variablesSection.getKeys(false).associateWith { key ->
            variablesSection[key]
        }
    }

    override fun toString(): String {
        return "KtsScript(key=$key, name=$name, version=$version, enabled=$enabled)"
    }
}
