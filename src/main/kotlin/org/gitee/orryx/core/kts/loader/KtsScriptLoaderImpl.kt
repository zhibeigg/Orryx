package org.gitee.orryx.core.kts.loader

import org.bukkit.plugin.Plugin
import org.gitee.orryx.core.kts.compiler.KtsCompiledScript
import org.gitee.orryx.core.kts.loader.classloader.ClassProvider
import org.gitee.orryx.core.kts.loader.classloader.ScriptClassloader
import org.gitee.orryx.core.kts.script.OrryxKtsScript
import org.gitee.orryx.utils.ktsNameRelative
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.loadDependencies

/**
 * Kts 脚本加载器实现
 *
 * 负责将已编译的脚本加载到内存中，创建脚本实例
 *
 * @property plugin 插件实例
 * @property scriptDir 脚本目录
 * @property parentClassloader 父类加载器
 * @property classProvider 类提供者（用于跨脚本类查找）
 * @property logger 日志记录函数
 */
class KtsScriptLoaderImpl(
    val plugin: Plugin,
    val scriptDir: File,
    val parentClassloader: ClassLoader,
    val classProvider: ClassProvider
) : KtsScriptLoader {

    /**
     * 加载已编译的脚本
     *
     * 加载过程：
     * 1. 创建脚本的数据文件夹
     * 2. 创建隔离的类加载器（包含脚本的依赖）
     * 3. 配置脚本求值环境（构造函数参数、类加载器）
     * 4. 使用 JVM 脚本求值器执行脚本
     * 5. 返回加载的脚本实例
     *
     * @param compiledScript 已编译的脚本
     * @return 已加载的脚本实例
     */
    override suspend fun load(compiledScript: KtsCompiledScript): KtsLoadedScript {
        // 创建脚本的数据文件夹（用于存储脚本的配置、数据等）
        val dataFolder = File(scriptDir, compiledScript.source.ktsNameRelative(scriptDir))

        // 创建脚本的隔离类加载器
        // 每个脚本有自己的类加载器，包含其 Maven 依赖
        val classLoader = ScriptClassloader(
            classProvider,
            parentClassloader,
            compiledScript.description.dependenciesFiles.map { File(it) }.toSet(),
        )

        // 配置脚本求值环境
        val evalConfig = ScriptEvaluationConfiguration {
            // 设置脚本构造函数参数（对应 OrryxKtsScript 的构造函数）
            constructorArgs(
                plugin,                         // 插件实例
                compiledScript.description,      // 脚本描述
                dataFolder,                      // 数据文件夹
                compiledScript.scriptName      // 脚本名称
            )
            jvm {
                // 使用脚本的隔离类加载器
                baseClassLoader(classLoader)
                // 不自动加载依赖（已通过类加载器处理）
                loadDependencies(false)
            }
        }

        // 创建 JVM 脚本求值器
        BasicJvmScriptEvaluator()

        // 执行脚本
        // 处理求值结果
        return when (val result = evaluator(compiledScript.compiled, evalConfig)) {
            is ResultWithDiagnostics.Success -> {
                when (val it = result.value.returnValue) {
                    // 如果脚本返回错误，抛出异常
                    is ResultValue.Error -> throw it.error
                    // 如果脚本未求值，抛出错误（TODO：实现具体的异常）
                    ResultValue.NotEvaluated -> TODO() // TODO: throw error
                    else -> {
                        // VALUE 和 UNIT 情况（正常返回）
                        if (it.scriptClass != null && it.scriptInstance !== null) {
                            // 创建已加载的脚本实例
                            KtsLoadedScript(
                                it.scriptInstance as OrryxKtsScript,  // 脚本实例
                                it.scriptClass!!,                     // 脚本类
                                classLoader,                          // 类加载器
                                compiledScript,                       // 已编译的脚本
                                dataFolder,                           // 数据文件夹
                            )
                        } else {
                            // 如果脚本类或实例为 null，抛出错误（TODO：实现具体的异常）
                            TODO() // TODO: throw error
                        }
                    }
                }
            }
            // 如果求值失败，抛出错误（TODO：实现具体的异常）
            is ResultWithDiagnostics.Failure -> TODO() // TODO: throw error
        }
    }
}
