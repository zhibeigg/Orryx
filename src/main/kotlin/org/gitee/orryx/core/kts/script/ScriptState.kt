package org.gitee.orryx.core.kts.script

import org.gitee.orryx.core.kts.compiler.KtsCompiledScript
import org.gitee.orryx.core.kts.loader.KtsLoadedScript
import taboolib.module.chat.colored
import java.io.File

/**
 * 脚本状态密封类
 * 定义了脚本在其生命周期中可能处于的所有状态
 *
 * 状态流程：
 * Discovered → CheckingCache → Compiling → Unloaded → Loading → Loaded
 *                                  ↓           ↓          ↓
 *                            CompileFail   Unloading  LoadFail
 *
 * @property scriptName 脚本名称
 */
sealed class ScriptState(
    val scriptName: String,
) {

    /**
     * 获取状态的显示名称（带颜色）
     */
    abstract fun stateDisplayName(): String

    /**
     * 已发现状态
     * 脚本文件存在，但尚未编译
     */
    class Discovered(
        scriptName: String,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&7Discovered".colored()
    }

    /**
     * 检查缓存状态
     * 正在检查是否存在有效的缓存编译结果
     */
    class CheckingCache(
        scriptName: String,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&bChecking Cache".colored()
    }

    /**
     * 编译中状态
     * 脚本正在被编译
     *
     * @property scriptFile 脚本文件
     * @property description 脚本描述信息
     */
    class Compiling(
        scriptName: String,
        val scriptFile: File,
        val description: ScriptDescription,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&bCompiling".colored()
    }

    /**
     * 加载中状态
     * 已编译的脚本正在被加载到内存中
     *
     * @property compiledScript 已编译的脚本
     */
    class Loading(
        scriptName: String,
        val compiledScript: KtsCompiledScript,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&bLoading".colored()
    }

    /**
     * 已加载状态
     * 脚本已成功加载并正在运行
     *
     * @property loadedScript 已加载的脚本实例
     */
    class Loaded(
        scriptName: String,
        val loadedScript: KtsLoadedScript,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&aLoaded".colored()
    }

    /**
     * 已卸载状态
     * 脚本已编译但未加载到内存中
     *
     * @property compiledScript 已编译的脚本
     */
    class Unloaded(
        scriptName: String,
        val compiledScript: KtsCompiledScript,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&eUnloaded".colored()
    }

    /**
     * 卸载中状态
     * 脚本正在被卸载
     *
     * @property compiledScript 已编译的脚本
     */
    class Unloading(
        scriptName: String,
        val compiledScript: KtsCompiledScript,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&bUnloading".colored()
    }

    /**
     * 编译失败状态
     * 脚本编译过程中发生错误
     *
     * @property error 错误信息
     */
    class CompileFail(
        scriptName: String,
        val error: String,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&4Compile File".colored()
    }

    /**
     * 加载失败状态
     * 脚本加载过程中捕获到异常
     *
     * @property error 错误信息
     */
    class LoadFail(
        scriptName: String,
        val error: String,
    ) : ScriptState(scriptName) {
        override fun stateDisplayName(): colored = "&4Load Fail".colored()
    }
}
