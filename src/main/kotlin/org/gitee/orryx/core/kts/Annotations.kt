package org.gitee.orryx.core.kts

/**
 * 定义脚本的描述
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class Script(
    val version: String = ""
)

/**
 * 用于导入和依赖其他脚本
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Import(
    vararg val script: String,
)

/**
 * 依赖插件
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependPlugin(
    vararg val plugin: String,
)