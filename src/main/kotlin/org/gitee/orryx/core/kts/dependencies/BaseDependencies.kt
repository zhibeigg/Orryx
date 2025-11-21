package org.gitee.orryx.core.kts.dependencies

data class Dependency(
    val fqnPackage: String,
    val repositoriesUrl: List<String>,
    val artifacts: List<String>,
)

val SPIGOT_DEPENDENCY = Dependency(
    "org.spigotmc",
    listOf(
        "https://hub.spigotmc.org/nexus/content/repositories/snapshots/",
        "https://oss.sonatype.org/content/repositories/snapshots/",
    ),
    listOf(
        "org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT",
    ),
)

val baseDependencies = listOf(
    SPIGOT_DEPENDENCY,
)