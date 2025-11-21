package org.gitee.orryx.core.kts.loader

import org.gitee.orryx.core.kts.compiler.KtsCompiledScript
import org.gitee.orryx.core.kts.loader.classloader.ScriptClassloader
import org.gitee.orryx.core.kts.script.OrryxKtsScript
import java.io.File
import kotlin.reflect.KClass

/**
 * 已加载的 Kts 脚本数据类
 *
 * @property script 脚本实例
 * @property kclass 脚本的 Kotlin 类
 * @property classLoader 脚本的类加载器（隔离的）
 * @property compiledScript 已编译的脚本
 * @property dataFolder 脚本的数据文件夹
 */
data class KtsLoadedScript(
    val script: OrryxKtsScript,
    val kclass: KClass<*>,
    val classLoader: ScriptClassloader,
    val compiledScript: KtsCompiledScript,
    val dataFolder: File,
)
