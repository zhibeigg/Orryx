package org.gitee.orryx.core.kts.resolver

import kotlinx.coroutines.runBlocking
import org.gitee.orryx.core.kts.configuration.classpathFromPlugins
import org.gitee.orryx.core.kts.dependencies.SPIGOT_DEPENDENCY
import org.gitee.orryx.core.kts.dependencies.baseDependencies
import org.gitee.orryx.core.kts.dependencies.ignoredPluginDependencies
import org.gitee.orryx.utils.findParentPluginFolder
import org.gitee.orryx.utils.isJar
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptSourceAnnotation
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.dependenciesSources
import kotlin.script.experimental.api.flatMapSuccess
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.api.with
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.dependencies.addRepository
import kotlin.script.experimental.dependencies.impl.resolve
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.resolveFromAnnotations
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.util.filterByAnnotationType

/**
 * 解析脚本的静态依赖
 * 这个函数在编译时被调用，用于设置脚本的类路径
 *
 * 工作流程：
 * 1. 检查是否在服务器环境中运行（通过检查 Spigot 包是否可用）
 * 2. 如果不在服务器中（IDE 环境）：
 *    - 从插件文件夹解析所有插件 JAR
 *    - 从服务器 JAR 中解析依赖
 *    - 解析基础依赖（KotlinBukkitAPI 等）
 * 3. 如果在服务器中：
 *    - 直接使用服务器的类路径
 *
 * @param ctx 脚本配置精化上下文
 * @return 更新后的编译配置
 */
fun resolveScriptStaticDependencies(
    ctx: ScriptConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    println("Comecei a resolver dependencia a script: resolveScriptStaticDependencies")
    val configuration = ctx.compilationConfiguration.with {
        // 如果此时 Spigot 不可用，说明正在 IDE 中运行，而不是在服务器上
        if (!isPackageAvailable(SPIGOT_DEPENDENCY.fqnPackage)) {
            val scriptFile = ctx.script.finalFile

            // Maven 依赖解析器（用于从 Maven 仓库下载依赖）
            val mavenResolver = MavenDependenciesResolver()
            // 文件系统依赖解析器（用于从本地文件系统解析 JAR）
            val fileResolver = FileSystemDependenciesResolver()

            val files = mutableListOf<File>()  // 编译后的依赖
            val sources = mutableListOf<File>() // 源代码依赖（用于 IDE）

            runBlocking {
                // 解析服务器/用户依赖
                val pluginsFolder = scriptFile.findParentPluginFolder(10)

                // 如果找到了插件文件夹，使用它来查找依赖；否则使用基础依赖
                if (pluginsFolder != null) {
                    // 获取所有插件 JAR（排除被忽略的插件）
                    val allPlugins = (pluginsFolder.listFiles() ?: emptyArray())
                        .filter { it.isJar() }
                        .filterNot { plugin ->
                            // 过滤掉不需要的插件（如 Orryx 自己）
                            ignoredPluginDependencies.any { plugin.name.contains(it, ignoreCase = true) }
                        }

                    // 获取服务器 JAR（通常在 plugins 文件夹的父目录）
                    val serverJar = (pluginsFolder.parentFile?.listFiles() ?: emptyArray())
                        .filter { it.isJar() }

                    // 将所有插件和服务器 JAR 添加到类路径
                    for (jar in allPlugins + serverJar) {
                        files += fileResolver.resolve(jar.absolutePath, mapOf()).valueOrNull() ?: emptyList()
                    }
                }

                // 解析基础 Maven 依赖（如 KotlinBukkitAPI、Kotlin 标准库等）
                for ((fqn, repositories, artifacts) in baseDependencies) {
                    // 只解析尚未可用的包
                    if (!isPackageAvailable(fqn)) {
                        // 添加所需的 Maven 仓库
                        for (repository in repositories) {
                            mavenResolver.addRepository(repository)
                        }

                        for (artifact in artifacts) {
                            // 如果没有找到插件文件夹，才添加编译依赖
                            // （否则从插件文件夹中已经获取了这些依赖）
                            if (pluginsFolder == null) {
                                files += mavenResolver.resolve(artifact, mapOf()).valueOrNull() ?: emptyList()
                            }

                            // 总是添加源代码（用于 IDE 中的代码导航和文档）
                            sources += mavenResolver.resolve(artifactAsSource(artifact), mapOf()).valueOrNull() ?: emptyList()
                        }
                    }
                }
            }

            // 更新脚本的类路径
            updateClasspath(files)

            // 为 IDE 添加源代码
            ide.dependenciesSources.append(JvmDependency(sources))
        } else {
            // 正在服务器上运行，直接使用插件的完整类路径
            updateClasspath(classpathFromPlugins())
        }
    }

    return configuration.asSuccess()
}

/**
 * 外部依赖数据类
 * @property compiled 编译后的依赖文件（JAR）
 * @property sources 源代码文件（用于 IDE）
 */
data class ExternalDependencies(
    val compiled: Set<File>,
    val sources: Set<File>,
)

/**
 * 解析外部依赖（通过 @file:Maven 和 @file:MavenRepository 注解）
 *
 * 这个函数用于解析脚本中通过注解声明的额外依赖：
 * - @file:Maven("group:artifact:version")
 * - @file:MavenRepository("repository-url")
 *
 * @param scriptSource 脚本源代码
 * @param annotations 脚本中的注解列表
 * @return 外部依赖（编译的和源代码的）
 */
fun resolveExternalDependencies(
    scriptSource: SourceCode,
    annotations: List<Annotation>,
): ExternalDependencies {
    // 如果在服务器上运行，使用服务器内部的缓存文件夹存储库
    // 缓存目录通过插件设置 user.home 属性来应用
    val mavenResolver = MavenDependenciesResolver()

    val sources = mutableSetOf<File>()

    // 检查 Spigot 是否可用，如果不可用，说明不在服务器上运行
    // 应该下载源代码（用于 IDE）
    if (!isPackageAvailable(SPIGOT_DEPENDENCY.fqnPackage)) {
        // 为 IntelliJ 下载源代码
        runBlocking {
            sources += mavenResolver.resolveSourceFromAnnotations(annotations).valueOrThrow()
        }
    }

    return runBlocking {
        ExternalDependencies(
            // 下载编译后的依赖
            mavenResolver.resolveFromAnnotations(annotations).valueOrThrow().toSet(),
            sources,
        )
    }
}

/**
 * 将 Maven 坐标转换为源代码坐标
 * 例如：将 "group:artifact:1.0" 转换为 "group:artifact:jar:sources:1.0"
 *
 * @param artifactsCoordinates Maven 坐标字符串
 * @return 源代码的 Maven 坐标
 */
private fun artifactAsSource(artifactsCoordinates: String): String {
    return if (artifactsCoordinates.count { it == ':' } == 2) {
        // 如果是标准的 group:artifact:version 格式
        val lastColon = artifactsCoordinates.lastIndexOf(':')
        // 在版本号前插入 ":jar:sources"
        artifactsCoordinates.toMutableList().apply { addAll(lastColon, ":jar:sources".toList()) }.joinToString("")
    } else {
        // 如果已经包含分类器或类型，直接返回
        artifactsCoordinates
    }
}

/**
 * 从脚本源注解中解析源代码依赖的扩展函数
 *
 * 这个函数处理 @Repository 和 @DependsOn 注解，并下载相应的源代码
 *
 * @param annotations 脚本源注解的可迭代集合
 * @return 解析结果，包含源代码文件列表或诊断信息
 */
suspend fun ExternalDependenciesResolver.resolveSourceFromScriptSourceAnnotations(
    annotations: Iterable<ScriptSourceAnnotation<*>>,
): ResultWithDiagnostics<List<File>> {
    val reports = mutableListOf<ScriptDiagnostic>()

    // 首先处理所有 @Repository 注解，配置仓库
    annotations.forEach { (annotation, locationWithId) ->
        when (annotation) {
            is Repository -> {
                // 添加所有配置的仓库
                for (coordinates in annotation.repositoriesCoordinates) {
                    val added = addRepository(coordinates, ExternalDependenciesResolver.Options.Empty, locationWithId)
                        .also { reports.addAll(it.reports) }
                        .valueOr { return it }

                    if (!added) {
                        // 如果仓库坐标无法识别，返回失败
                        return reports + makeFailureResult(
                            "Unrecognized repository coordinates: $coordinates",
                            locationWithId = locationWithId,
                        )
                    }
                }
            }
            is DependsOn -> {} // DependsOn 注解稍后处理
            else -> return reports + makeFailureResult("Unknown annotation ${annotation.javaClass}", locationWithId = locationWithId)
        }
    }

    // 然后处理所有 @DependsOn 注解，下载源代码
    return reports + annotations.filterByAnnotationType<DependsOn>()
        .flatMapSuccess { (annotation, locationWithId) ->
            annotation.artifactsCoordinates.asIterable().flatMapSuccess { artifactCoordinates ->
                // 为每个依赖下载源代码
                resolve(artifactAsSource(artifactCoordinates), ExternalDependenciesResolver.Options.Empty, locationWithId)
            }
        }
}

/**
 * 从普通注解中解析源代码依赖的扩展函数
 *
 * 将普通注解转换为脚本源注解，然后调用 resolveSourceFromScriptSourceAnnotations
 *
 * @param annotations 注解的可迭代集合
 * @return 解析结果，包含源代码文件列表或诊断信息
 */
suspend fun ExternalDependenciesResolver.resolveSourceFromAnnotations(
    annotations: Iterable<Annotation>,
): ResultWithDiagnostics<List<File>> {
    // 将普通注解转换为脚本源注解（不带位置信息）
    val scriptSourceAnnotations = annotations.map { ScriptSourceAnnotation(it, null) }
    return resolveSourceFromScriptSourceAnnotations(scriptSourceAnnotations)
}
