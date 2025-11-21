package org.gitee.orryx.core.kts.resolver

import org.gitee.orryx.core.kts.DependPlugin
import org.gitee.orryx.core.kts.Script
import org.gitee.orryx.core.kts.configuration.info
import org.gitee.orryx.core.kts.script.ScriptDescription
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.updateClasspath

fun resolveScriptAnnotation(
    ctx: ScriptConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    println("resolvendo as merdas das anotacoes: resolveScriptAnnotation")
    val annotations = ctx.collectedData?.get(ScriptCollectedData.foundAnnotations)
        ?.takeIf { it.isNotEmpty() } ?: return ctx.compilationConfiguration.asSuccess()

    val reports = mutableListOf<ScriptDiagnostic>()

    val configuration = ctx.compilationConfiguration.with {
        var version = "Unknown"

        val pluginDepends = mutableSetOf<String>()

        for (annotation in annotations) {
            when (annotation) {
                is Script -> {
                    annotation.version.takeIf { it.isNotBlank() }?.also { version = it }
                }

                is DependPlugin -> pluginDepends.addAll(annotation.plugin)
            }
        }

        // 解析/下载注解提供的外部依赖（@file:Maven 和 @file:MavenRepository）
        // 将依赖添加到编译类路径，并保存到 ScriptDescription 中供类加载器使用
        val external = resolveExternalDependencies(ctx.script, annotations)
        val dependenciesFiles = external.compiled

        if (dependenciesFiles.isNotEmpty()) {
            updateClasspath(dependenciesFiles)
        }

        info(
            ScriptDescription(
                version,
                pluginDepends,
                dependenciesFiles.map { it.absolutePath }.toSet(),
            ),
        )

        if (external.sources.isNotEmpty()) {
            ide.dependenciesSources.append(JvmDependency(external.sources.toList()))
        }
    }

    return configuration.asSuccess()
}
