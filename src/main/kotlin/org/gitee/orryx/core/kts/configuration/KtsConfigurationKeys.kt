package org.gitee.orryx.core.kts.configuration

import org.gitee.orryx.core.kts.script.ScriptDescription
import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

typealias CompilerKeys = ScriptCompilationConfigurationKeys

val CompilerKeys.info by PropertiesCollection.key(
    ScriptDescription(
        "Unknown",
        emptySet(),
        emptySet(),
    ),
)
