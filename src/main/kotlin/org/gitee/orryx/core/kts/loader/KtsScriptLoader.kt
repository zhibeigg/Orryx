package org.gitee.orryx.core.kts.loader

import org.gitee.orryx.core.kts.KtsLoadException
import org.gitee.orryx.core.kts.compiler.KtsCompiledScript

/** 脚本路径类型别名 */
typealias ScriptPath = String

/**
 * Kts 脚本加载器接口
 * 负责将已编译的脚本加载到内存并实例化
 */
interface KtsScriptLoader {
    /**
     * 加载已编译的脚本
     *
     * @param compiledScript 已编译的脚本
     * @return 已加载的脚本实例
     * @throws KtsLoadException 如果加载失败
     */
    @Throws(KtsLoadException::class)
    suspend fun load(compiledScript: KtsCompiledScript): KtsLoadedScript
}
