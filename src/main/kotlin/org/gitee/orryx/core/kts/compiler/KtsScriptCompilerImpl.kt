package org.gitee.orryx.core.kts.compiler

import org.gitee.orryx.core.kts.KtsCompilationException
import org.gitee.orryx.core.kts.cache.CachedScript
import org.gitee.orryx.core.kts.cache.FileBasedScriptCache
import org.gitee.orryx.core.kts.configuration.info
import org.gitee.orryx.core.kts.script.OrryxKtsScript
import org.gitee.orryx.core.kts.script.ScriptDescription
import org.gitee.orryx.utils.ktsNameRelative
import java.io.File
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.api.with
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

/**
 * Kts 脚本编译器实现
 *
 * @property scriptDir 脚本目录
 * @property cacheDir 编译缓存目录
 */
class KtsScriptCompilerImpl(
    val scriptDir: File,
    val cacheDir: File,
) : KtsScriptCompiler {

    /**
     * 检索脚本描述信息（不完全编译）
     *
     * 这个方法使用一个技巧：它开始编译脚本，但在 beforeCompiling 阶段
     * 提取脚本描述信息后立即失败，避免完整编译以提高性能
     *
     * @param scriptFile 脚本文件
     * @return 脚本描述信息，如果无法检索则返回 null
     */
    override suspend fun retrieveDescriptor(scriptFile: File): ScriptDescription? {
        var scriptDescriptionLoaded: ScriptDescription? = null

        println("Tentando fazer o retrieveDescriptor antes de realmente compilar rs rs rs")
        // 创建自定义编译配置，用于在编译前提取脚本信息
        val customConfiguration =
            createJvmCompilationConfigurationFromTemplate<OrryxKtsScript>().with {
                refineConfiguration {
                    println("O refineConfiguration foi de comes e executes?")
//                    beforeParsing {
//                        println("Oh no before parsing foi chamado corretamente")
//                        return@beforeParsing ResultWithDiagnostics.Success(it.compilationConfiguration)
//                    }
                    // 在编译前拦截，提取脚本描述信息
                    beforeCompiling { context ->
                        println("beforeCompiling refine mother fucker")
                        // 从编译配置中获取脚本描述信息
                        val info = context.compilationConfiguration[ScriptCompilationConfiguration.info]!!

                        scriptDescriptionLoaded = info

                        // 立即返回失败，避免完整编译
                        return@beforeCompiling ResultWithDiagnostics.Failure()
                    }
                }
            }

        val source = FileScriptSource(scriptFile)

        // 尝试编译（实际上只是为了触发 beforeCompiling 钩子）
        runCatching {
            println("Tentando fazer aquela compilacao!")
            compile(source, customConfiguration).valueOrThrow()
            println("Aquela compilacao foi de OKAY")
        }.onFailure {
            println("Aquela compilacao foi de comes e bebes")
            // 如果脚本描述未加载，则重新抛出异常
            if (scriptDescriptionLoaded == null) {
                throw it
            }
        }

        return scriptDescriptionLoaded
    }

    /**
     * 完整编译脚本
     *
     * @param scriptFile 脚本文件
     * @param description 脚本描述信息
     * @return 已编译的脚本
     * @throws KtsCompilationException 如果编译失败
     */
    override suspend fun compile(scriptFile: File, description: ScriptDescription): KtsCompiledScript {
        val source = FileScriptSource(scriptFile)

        // 创建基于文件的脚本缓存
        val cache = FileBasedScriptCache(scriptDir, cacheDir, description)

        // 创建编译配置
        val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<OrryxKtsScript>()

        // 配置主机环境，启用编译缓存
        val hostConfiguration = defaultJvmScriptingHostConfiguration.with {
            jvm {
                compilationCache(cache)
            }
            this.configurationDependencies
        }

        return runCatching {
            // 执行编译
            val compiledScript = compile(source, compilationConfiguration, hostConfiguration)
                .valueOrThrow() as KJvmCompiledScript

            KtsCompiledScript(
                scriptFile.ktsNameRelative(scriptDir),
                source,
                compiledScript,
                description,
            )
        }.getOrElse {
            throw KtsCompilationException(it)
        }
    }

    /**
     * 获取缓存的脚本
     *
     * @param scriptFile 脚本文件
     * @return 缓存的脚本，如果不存在或无效则返回 null
     */
    override suspend fun getCachedScript(scriptFile: File): CachedScript? {
        val scriptCache = FileBasedScriptCache(scriptDir, cacheDir, null)

        val source = FileScriptSource(scriptFile)

        return scriptCache.findCacheScript(source)
    }

    /**
     * 底层编译方法
     *
     * @param source 脚本源文件
     * @param configuration 编译配置
     * @param hostConfiguration 主机配置
     * @return 编译结果（成功或失败及诊断信息）
     */
    private suspend fun compile(
        source: FileScriptSource,
        configuration: ScriptCompilationConfiguration,
        hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    ): ResultWithDiagnostics<CompiledScript> {
        // 创建 JVM 脚本编译器
        val compiler = JvmScriptCompiler(hostConfiguration)

        // 执行编译
        return compiler(source, configuration)
    }
}
