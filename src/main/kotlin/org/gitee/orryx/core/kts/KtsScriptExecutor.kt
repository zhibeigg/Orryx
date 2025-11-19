package org.gitee.orryx.core.kts

import kotlinx.coroutines.future.future
import org.bukkit.plugin.Plugin
import org.gitee.orryx.api.OrryxAPI
import taboolib.common.platform.function.warning
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * KTS 脚本执行器
 * 负责脚本的执行和结果处理
 */
class KtsScriptExecutor(private val plugin: Plugin) {

    /**
     * 脚本宿主
     */
    private val scriptingHost = BasicJvmScriptingHost()

    /**
     * 执行脚本文件
     *
     * @param file 脚本文件
     * @param context 脚本上下文
     * @param timeout 超时时间(毫秒), 0 表示不限制
     * @return 执行结果
     */
    suspend fun execute(file: File, context: KtsScriptContext, timeout: Long = 0): KtsExecutionResult {
        // 编译脚本
        val compilationResult = KtsScriptCompiler.compile(file)
        if (compilationResult !is ResultWithDiagnostics.Success) {
            return KtsExecutionResult.failure("编译失败", compilationResult.reports)
        }

        return executeCompiled(compilationResult.value, context, timeout)
    }

    /**
     * 执行脚本代码
     *
     * @param code 脚本代码
     * @param name 脚本名称
     * @param context 脚本上下文
     * @param timeout 超时时间(毫秒), 0 表示不限制
     * @return 执行结果
     */
    suspend fun executeCode(code: String, name: String = "script", context: KtsScriptContext, timeout: Long = 0): KtsExecutionResult {
        // 编译脚本
        val compilationResult = KtsScriptCompiler.compileCode(code, name)
        if (compilationResult !is ResultWithDiagnostics.Success) {
            return KtsExecutionResult.failure("编译失败", compilationResult.reports)
        }

        return executeCompiled(compilationResult.value, context, timeout)
    }

    /**
     * 执行已编译的脚本
     */
    private suspend fun executeCompiled(compiled: CompiledScript, context: KtsScriptContext, timeout: Long): KtsExecutionResult {
        // 创建评估配置
        val evaluationConfiguration = ScriptEvaluationConfiguration {
            jvm {
                baseClassLoader(plugin.javaClass.classLoader)
            }
            // 提供脚本实例配置器
            constructorArgs(context)
            providedProperties(
                "plugin" to plugin,
                "player" to context.player,
                "context" to context
            )
        }

        return try {
            if (timeout > 0) {
                // 带超时的异步执行
                val future = OrryxAPI.ioScope.future {
                    scriptingHost.evaluator(compiled, evaluationConfiguration)
                }

                val result = future.get(timeout, TimeUnit.MILLISECONDS)
                processResult(result)
            } else {
                // 同步执行
                val result = scriptingHost.evaluator(compiled, evaluationConfiguration)
                processResult(result)
            }
        } catch (e: java.util.concurrent.TimeoutException) {
            warning("脚本执行超时 (${timeout}ms)")
            KtsExecutionResult.failure("脚本执行超时", emptyList())
        } catch (e: Exception) {
            warning("脚本执行异常: ${e.message}")
            e.printStackTrace()
            KtsExecutionResult.failure("执行异常: ${e.message}", emptyList())
        }
    }

    /**
     * 处理执行结果
     */
    private fun processResult(result: ResultWithDiagnostics<EvaluationResult>): KtsExecutionResult {
        return when (result) {
            is ResultWithDiagnostics.Success -> {
                when (val returnValue = result.value.returnValue) {
                    is ResultValue.Value -> KtsExecutionResult.success(returnValue.value, result.reports)
                    is ResultValue.Unit -> KtsExecutionResult.success(Unit, result.reports)
                    is ResultValue.Error -> {
                        warning("脚本执行错误: ${returnValue.error.message}")
                        returnValue.error.printStackTrace()
                        KtsExecutionResult.failure("执行错误: ${returnValue.error.message}", result.reports)
                    }
                    is ResultValue.NotEvaluated -> KtsExecutionResult.failure("脚本未被评估", result.reports)
                }
            }
            is ResultWithDiagnostics.Failure -> {
                KtsExecutionResult.failure("执行失败", result.reports)
            }
        }
    }
}

/**
 * KTS 脚本执行结果
 */
sealed class KtsExecutionResult {
    /**
     * 执行成功
     */
    data class Success(
        val value: Any?,
        val diagnostics: List<ScriptDiagnostic>
    ) : KtsExecutionResult() {
        val hasWarnings: Boolean
            get() = diagnostics.any { it.severity == ScriptDiagnostic.Severity.WARNING }
    }

    /**
     * 执行失败
     */
    data class Failure(
        val message: String,
        val diagnostics: List<ScriptDiagnostic>
    ) : KtsExecutionResult() {
        val errors: List<ScriptDiagnostic>
            get() = diagnostics.filter { it.severity == ScriptDiagnostic.Severity.ERROR }
    }

    /**
     * 是否成功
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 是否失败
     */
    val isFailure: Boolean
        get() = this is Failure

    /**
     * 获取返回值(如果成功)
     */
    fun getOrNull(): Any? {
        return when (this) {
            is Success -> value
            is Failure -> null
        }
    }

    /**
     * 获取返回值或默认值
     */
    fun getOrDefault(default: Any?): Any? {
        return when (this) {
            is Success -> value ?: default
            is Failure -> default
        }
    }

    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String? {
        return when (this) {
            is Success -> null
            is Failure -> message
        }
    }

    /**
     * 打印诊断信息
     */
    fun printDiagnostics() {
        when (this) {
            is Success -> {
                diagnostics.forEach { diagnostic ->
                    when (diagnostic.severity) {
                        ScriptDiagnostic.Severity.WARNING -> warning("[警告] ${diagnostic.message}")
                        ScriptDiagnostic.Severity.ERROR -> warning("[错误] ${diagnostic.message}")
                        else -> {}
                    }
                }
            }
            is Failure -> {
                warning("[失败] $message")
                diagnostics.forEach { diagnostic ->
                    warning("  ${diagnostic.severity}: ${diagnostic.message}")
                }
            }
        }
    }

    companion object {
        fun success(value: Any?, diagnostics: List<ScriptDiagnostic> = emptyList()): KtsExecutionResult {
            return Success(value, diagnostics)
        }

        fun failure(message: String, diagnostics: List<ScriptDiagnostic> = emptyList()): KtsExecutionResult {
            return Failure(message, diagnostics)
        }
    }
}
