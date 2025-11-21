package org.gitee.orryx.core.kts.compiler

import org.gitee.orryx.core.kts.KtsCompilationException
import org.gitee.orryx.core.kts.cache.CachedScript
import org.gitee.orryx.core.kts.script.ScriptDescription
import java.io.File

/**
 * Kts 脚本编译器接口
 *
 * 负责脚本的编译和缓存管理
 */
interface KtsScriptCompiler {

    /**
     * 检索脚本描述信息（不完全编译）
     * 从脚本文件中提取元数据，避免完整编译以提高性能
     *
     * @param scriptFile 脚本文件
     * @return 脚本描述信息，如果无法检索则返回 null
     */
    suspend fun retrieveDescriptor(scriptFile: File): ScriptDescription?

    /**
     * 完整编译脚本
     *
     * @param scriptFile 脚本文件
     * @param description 脚本描述信息
     * @return 已编译的脚本
     * @throws KtsCompilationException 如果编译失败
     */
    @Throws(KtsCompilationException::class)
    suspend fun compile(scriptFile: File, description: ScriptDescription): KtsCompiledScript

    /**
     * 获取缓存的脚本
     *
     * @param scriptFile 脚本文件
     * @return 缓存的脚本，如果不存在或无效则返回 null
     */
    suspend fun getCachedScript(scriptFile: File): CachedScript?
}
