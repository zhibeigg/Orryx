package org.gitee.orryx.core.kts.script

import org.gitee.orryx.OrryxPlugin
import org.gitee.orryx.core.kts.configuration.KtsScriptCompilationConfiguration
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript

const val KTS_EXTENSION = "kts"

// 用于移除注册的函数类型别名
typealias RemoveRegistryFunction = () -> Unit

/**
 * Kts 脚本基类
 *
 * 所有 .kts 脚本都会继承此类
 *
 * @property plugin 插件实例
 * @property description 脚本描述信息（版本、作者、日志级别等）
 * @property dataFolder 脚本的数据文件夹
 * @property scriptName 脚本名称（不含扩展名）
 * @property log 日志记录函数
 */
@KotlinScript(
    displayName = "Orryx Kts script",
    fileExtension = KTS_EXTENSION,
    compilationConfiguration = KtsScriptCompilationConfiguration::class,
)
abstract class OrryxKtsScript(
    val plugin: OrryxPlugin,
    val description: ScriptDescription,
    val dataFolder: File,
    val scriptName: String
) {

    private val _onDisableListeners = mutableListOf<() -> Unit>()
    val onDisableListeners: List<() -> Unit> = _onDisableListeners

    /**
     * 注册脚本卸载时的回调函数
     * 当脚本被卸载或重新加载时，会调用所有注册的回调
     *
     * @param callback 卸载时要执行的回调函数
     * @return 返回一个函数，调用该函数可以移除此回调
     */
    fun onDisable(callback: () -> Unit): RemoveRegistryFunction {
        _onDisableListeners.add(callback)

        return { _onDisableListeners.remove(callback) }
    }
}
