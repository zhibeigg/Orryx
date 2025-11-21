package org.gitee.orryx.core.kts

// Bukkit API 的所有核心包
val bukkitImports = listOf(
    "org.bukkit.*",
    "org.bukkit.block.*",
    "org.bukkit.block.banner.*",
    "org.bukkit.command.*",
    "org.bukkit.configuration.*",
    "org.bukkit.configuration.file.*",
    "org.bukkit.configuration.serialization.*",
    "org.bukkit.enchantments.*",
    "org.bukkit.entity.*",
    "org.bukkit.entity.minecart.*",
    "org.bukkit.event.*",
    "org.bukkit.event.block.*",
    "org.bukkit.event.enchantment.*",
    "org.bukkit.event.entity.*",
    "org.bukkit.event.hanging.*",
    "org.bukkit.event.inventory.*",
    "org.bukkit.event.painting.*",
    "org.bukkit.event.player.*",
    "org.bukkit.event.server.*",
    "org.bukkit.event.weather.*",
    "org.bukkit.event.world.*",
    "org.bukkit.generator.*",
    "org.bukkit.help.*",
    "org.bukkit.inventory.*",
    "org.bukkit.inventory.meta.*",
    "org.bukkit.map.*",
    "org.bukkit.material.*",
    "org.bukkit.metadata.*",
    "org.bukkit.permissions.*",
    "org.bukkit.plugin.*",
    "org.bukkit.plugin.messaging.*",
    "org.bukkit.potion.*",
    "org.bukkit.projectiles.*",
    "org.bukkit.scheduler.*",
    "org.bukkit.scoreboard.*",
    "org.bukkit.util.*",
    "org.bukkit.util.io.*",
    "org.bukkit.util.noise.*",
    "org.bukkit.util.permissions.*",
)

// Kotlin 标准库的常用导入
val kotlinImports = listOf(
    "kotlin.time.*",
    "kotlin.math.*",
)

// Java 标准库的常用导入
val javaImports = listOf(
    "java.util.*",
    "java.util.concurrent.*",
    "java.io.*",
    // "java.lang.*",
)

// Kotlin 协程的导入
// 提供异步编程支持
val kotlinCoroutinesImports = listOf(
    "kotlinx.coroutines.*",
    "kotlinx.coroutines.flow.*",
    "kotlinx.coroutines.channels.*",
    "kotlinx.coroutines.selects.*",
)

// Kotlin 脚本依赖注解的导入
// 允许脚本使用 @DependsOn 和 @Repository 注解添加外部依赖
val scriptingImports = listOf(
    "kotlin.script.experimental.dependencies.DependsOn",
    "kotlin.script.experimental.dependencies.Repository",
)
