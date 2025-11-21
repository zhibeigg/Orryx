package org.gitee.orryx.core.kts.compiler

import org.gitee.orryx.core.kts.script.ScriptDescription
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript

/**
 * 已编译的 Kts 脚本数据类
 *
 * @property scriptName 脚本名称（不含扩展名）
 * @property source 脚本源文件
 * @property compiled Kotlin 编译后的脚本对象
 * @property description 脚本描述信息
 */
data class KtsCompiledScript(
    val scriptName: String,
    val source: FileScriptSource,
    val compiled: KJvmCompiledScript,
    val description: ScriptDescription,
)
