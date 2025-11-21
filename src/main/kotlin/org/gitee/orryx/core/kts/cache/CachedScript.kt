package org.gitee.orryx.core.kts.cache

import org.gitee.orryx.core.kts.compiler.KtsCompiledScript
import java.io.File

data class CachedScript(
    val cacheFile: File,
    val isValid: Boolean,
    val compiled: KtsCompiledScript,
)
