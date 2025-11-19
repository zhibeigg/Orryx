package org.gitee.orryx.core.kts

import taboolib.common.platform.function.warning
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.script.experimental.api.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * KTS 脚本编译器
 * 负责脚本的编译、缓存和验证
 */
object KtsScriptCompiler {

    /**
     * 脚本编译配置
     */
    private val compilationConfiguration = KtsScriptCompilationConfiguration

    /**
     * 脚本评估配置
     */
    private val evaluationConfiguration = KtsScriptEvaluationConfiguration

    /**
     * 脚本宿主
     */
    private val scriptingHost = BasicJvmScriptingHost()

    /**
     * 编译缓存 (脚本Hash -> 编译结果)
     */
    private val compilationCache = ConcurrentHashMap<String, CompiledScript>()

    /**
     * 文件Hash缓存 (文件路径 -> Hash值)
     */
    private val fileHashCache = ConcurrentHashMap<String, String>()

    /**
     * 编译脚本文件
     *
     * @param file 脚本文件
     * @param useCache 是否使用缓存
     * @return 编译结果
     */
    suspend fun compile(file: File, useCache: Boolean = true): ResultWithDiagnostics<CompiledScript> {
        if (!file.exists()) {
            return ResultWithDiagnostics.Failure("脚本文件不存在: ${file.path}".asErrorDiagnostics())
        }

        val scriptSource = file.toScriptSource()
        val scriptHash = calculateHash(file)

        // 检查缓存
        if (useCache) {
            val cachedHash = fileHashCache[file.path]
            if (cachedHash == scriptHash) {
                compilationCache[scriptHash]?.let {
                    return ResultWithDiagnostics.Success(it)
                }
            }
        }

        // 编译脚本
        return try {
            val result = scriptingHost.compiler(scriptSource, compilationConfiguration)

            // 更新缓存
            if (result is ResultWithDiagnostics.Success) {
                compilationCache[scriptHash] = result.value
                fileHashCache[file.path] = scriptHash
            }

            result
        } catch (e: Exception) {
            warning("编译脚本失败: ${file.path}")
            e.printStackTrace()
            ResultWithDiagnostics.Failure(e.message?.asErrorDiagnostics() ?: "未知编译错误".asErrorDiagnostics())
        }
    }

    /**
     * 编译脚本代码
     *
     * @param code 脚本代码
     * @param name 脚本名称
     * @param useCache 是否使用缓存
     * @return 编译结果
     */
    suspend fun compileCode(code: String, name: String = "script", useCache: Boolean = true): ResultWithDiagnostics<CompiledScript> {
        val scriptSource = code.toScriptSource(name)
        val scriptHash = calculateHash(code)

        // 检查缓存
        if (useCache && compilationCache.containsKey(scriptHash)) {
            return ResultWithDiagnostics.Success(compilationCache[scriptHash]!!)
        }

        // 编译脚本
        return try {
            val result = scriptingHost.compiler(scriptSource, compilationConfiguration)

            // 更新缓存
            if (result is ResultWithDiagnostics.Success) {
                compilationCache[scriptHash] = result.value
            }

            result
        } catch (e: Exception) {
            warning("编译脚本失败: $name")
            e.printStackTrace()
            ResultWithDiagnostics.Failure(e.message?.asErrorDiagnostics() ?: "未知编译错误".asErrorDiagnostics())
        }
    }

    /**
     * 计算文件Hash值
     */
    private fun calculateHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = file.readBytes()
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 计算字符串Hash值
     */
    private fun calculateHash(content: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = content.toByteArray()
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 清空编译缓存
     */
    fun clearCache() {
        compilationCache.clear()
        fileHashCache.clear()
    }

    /**
     * 清空指定文件的缓存
     */
    fun clearCache(file: File) {
        fileHashCache.remove(file.path)?.let { hash ->
            compilationCache.remove(hash)
        }
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Int {
        return compilationCache.size
    }

    /**
     * 字符串转错误诊断信息
     */
    private fun String.asErrorDiagnostics(): List<ScriptDiagnostic> {
        return listOf(
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                this,
                severity = ScriptDiagnostic.Severity.ERROR
            )
        )
    }
}

/**
 * KTS 脚本基类
 * 所有脚本都继承此类
 */
@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = KtsScriptCompilationConfiguration::class,
    evaluationConfiguration = KtsScriptEvaluationConfiguration::class
)
abstract class KtsScript {
    /**
     * 脚本上下文
     */
    lateinit var context: KtsScriptContext
}

/**
 * 脚本编译配置
 */
object KtsScriptCompilationConfiguration : ScriptCompilationConfiguration({
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    defaultImports(
        "org.bukkit.*",
        "org.bukkit.entity.*",
        "org.bukkit.inventory.*",
        "taboolib.common.*",
        "taboolib.common.platform.*",
        "taboolib.module.chat.*",
        "taboolib.platform.util.*",
        "org.gitee.orryx.api.*"
    )
})

/**
 * 脚本评估配置
 */
object KtsScriptEvaluationConfiguration : ScriptEvaluationConfiguration({
    jvm {
        // 基础 JVM 配置
    }
})
