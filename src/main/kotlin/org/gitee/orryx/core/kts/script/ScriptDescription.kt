package org.gitee.orryx.core.kts.script

import java.io.Serializable

data class ScriptDescription(
    val version: String,
    val pluginDependencies: Set<String>,
    val dependenciesFiles: Set<String>,
) : Serializable
