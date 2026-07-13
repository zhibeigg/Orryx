import groovy.json.JsonSlurper
import io.izzel.taboolib.gradle.*
import xyz.jpenilla.runpaper.task.RunServer

val publishUsername: String by project
val publishPassword: String by project
val build: String by project
val token: String by project

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("io.izzel.taboolib") version "2.0.37"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("org.jetbrains.dokka") version "2.0.0"
}

val ketherDocsSiteDirectory = layout.buildDirectory.dir("generated-docs")
val ketherDocsServerDirectory = layout.buildDirectory.dir("kether-docs-server")

/**
 * 启动临时 Paper 服务端，复用插件完整生命周期注册的 Wiki 数据生成 Kether 文档。
 */
tasks.register<RunServer>("generateKetherDocs") {
    group = "documentation"
    description = "Builds Orryx, starts a temporary Paper server, and generates Kether documentation."

    dependsOn(tasks.named("jar"))
    minecraftVersion("1.21.1")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    runDirectory.set(ketherDocsServerDirectory)
    pluginJars(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    systemProperty("com.mojang.eula.agree", "true")
    systemProperty("orryx.ketherDocs.version", project.version.toString())

    inputs.dir("src/main/kotlin")
    inputs.dir("src/main/resources")
    inputs.file("build.gradle.kts")
    inputs.file("gradle.properties")
    outputs.dir(ketherDocsSiteDirectory)

    doFirst {
        val outputDirectory = ketherDocsSiteDirectory.get().asFile
        project.delete(outputDirectory)
        systemProperty("orryx.ketherDocs.output", outputDirectory.absolutePath)
        providers.environmentVariable("GITHUB_SHA").orNull
            ?.takeIf { it.isNotBlank() }
            ?.let { systemProperty("orryx.ketherDocs.commit", it) }
    }

    doLast {
        val siteDirectory = ketherDocsSiteDirectory.get().asFile
        val outputDirectory = siteDirectory.resolve("kether")
        val latest = outputDirectory.resolve("latest.md")
        val schema = outputDirectory.resolve("actions-schema.json")
        val manifest = outputDirectory.resolve("manifest.json")
        val versioned = outputDirectory.resolve("versions/${project.version}.md")

        listOf(latest, schema, manifest, versioned, siteDirectory.resolve("index.html")).forEach { file ->
            check(file.isFile && file.length() > 0L) {
                "Kether documentation output is missing or empty: ${file.absolutePath}"
            }
        }
        check(siteDirectory.resolve(".nojekyll").isFile) {
            "GitHub Pages marker is missing: ${siteDirectory.resolve(".nojekyll").absolutePath}"
        }
        runCatching { JsonSlurper().parse(schema) }
            .getOrElse { throw GradleException("Invalid actions-schema.json", it) }
        runCatching { JsonSlurper().parse(manifest) }
            .getOrElse { throw GradleException("Invalid manifest.json", it) }
    }
}

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitFakeOp)
        install(BukkitHook)
        install(BukkitNavigation)
        install(BukkitUI)
        install(BukkitUtil)
        install(BukkitNMS)
        install(BukkitNMSUtil)
        install(BukkitNMSItemTag)
        install(BukkitNMSDataSerializer)
        install(BukkitNMSEntityAI)
        install(MinecraftChat)
        install(MinecraftEffect)
        install(I18n)
        install(Metrics)
        install(CommandHelper)
        install(Database)
        install(Kether)
        install(Jexl)
        install("database-h2")
        //repoTabooLib = "https://maven.mcwar.cn/releases"
    }
    description {
        name = "Orryx"
        desc("跨时代技能插件，支持实现复杂逻辑，为稳定高效而生")
        contributors {
            name("zhibei")
        }
        links {
            name("homepage").url = "https://orryx.mcwar.cn/"
        }
        dependencies {
            name("Adyeshach").optional(true)
            name("DragonCore").optional(true)
            name("DragonArmourers").optional(true)
            name("GermPlugin").optional(true)
            name("ArcartX").optional(true)
            name("MythicMobs").optional(true)
            name("RedisChannel").optional(true)
            name("AttributePlus").optional(true)
            name("AstraXHero").optional(true)
            name("packetevents").optional(true)
            name("ProtocolLib").optional(true)
            name("GDDTitle").optional(true)
            name("PlaceholderAPI").optional(true)
            name("GlowAPI").optional(true)
            name("DungeonPlus").optional(true)
            name("Nodens").optional(true)
            name("CloudPick").optional(true)
            name("CraneAttribute").optional(true)
        }
    }
    relocate("com.github.benmanes.caffeine", "org.gitee.orryx.caffeine")
    relocate("org.joml", "org.gitee.orryx.joml")
    relocate("com.larksuite.oapi", "org.gitee.orryx.larksuite.oapi")
    relocate("com.eatthepath.uuid", "org.gitee.orryx.eatthepath.uuid")
    relocate("kotlinx.serialization", "org.gitee.orryx.serialization")
    relocate("org.java_websocket", "org.gitee.orryx.java_websocket")
    version {
        taboolib = "6.3.0-932e79c"
        coroutines = "1.10.1"
    }
}

repositories {
    mavenCentral()
    maven("https://maven.mcwar.cn/releases")
    maven("https://repo.codemc.io/repository/maven-releases")
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("org.gitee.nodens:nodens:1.26.43:api")
    compileOnly("com.gitee.redischannel:RedisChannel:1.14.11:api")
    compileOnly("ink.ptms.adyeshach:api:2.1.28")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("org.eldergod.ext:DragonCore:2.6.2.9")
    compileOnly("org.eldergod.ext:GermPlugin:4.4.1-5")
    compileOnly("org.eldergod.ext:DragonArmourers:6.72")
    compileOnly("org.eldergod.ext:MythicMobs:4.11.0") {
        exclude("com.google.guava", "guava")
    }

    compileOnly("org.eldergod.ext:GDDTitle:2.1")
    compileOnly("org.eldergod.ext:GlowAPI:1.4.6")
    compileOnly("org.eldergod.ext:AttributePlus-api:1.0")
    compileOnly("org.eldergod.ext:DungeonPlus:1.4.3")
    compileOnly("net.dmulloy2:ProtocolLib:5.3.0")
    compileOnly("org.eldergod.ext:CloudPick:1.2.1")

    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    compileOnly("org.joml:joml:1.10.7")
    compileOnly("com.larksuite.oapi:oapi-sdk:2.4.22")
    compileOnly("com.eatthepath:fast-uuid:0.2.0")
    compileOnly("org.java-websocket:Java-WebSocket:1.5.7")

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testImplementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    testImplementation("com.eatthepath:fast-uuid:0.2.0")
    testImplementation(kotlin("reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.joml:joml:1.10.7")
    testImplementation("ink.ptms.core:v12004:12004:mapped")
    testImplementation("ink.ptms.core:v12004:12004:universal")

    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")

    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))
    compileOnly(fileTree("libs"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    destinationDirectory.set(File(build))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.mcwar.cn/releases")
            credentials {
                username = publishUsername
                password = publishPassword
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifact(tasks["kotlinSourcesJar"]) {
                classifier = "sources"
            }
            artifact("${build}/${rootProject.name}-${version}-api.jar") {
                classifier = "api"
            }
            groupId = project.group.toString()
            artifactId = "orryx"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/zhibeigg/Orryx")
            credentials {
                username = "zhibeigg"
                password = token
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
            artifact(tasks["kotlinSourcesJar"]) {
                classifier = "sources"
            }
            artifact("${build}/${rootProject.name}-${version}-api.jar") {
                classifier = "api"
            }
            groupId = project.group.toString()
            artifactId = "orryx"
        }
    }
}

tasks.register("dokkaHtml") {
    group = "documentation"
    description = "Generates HTML API documentation."
    dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
}

dokka {
    moduleName.set("Orryx")

    dokkaPublications.html {
        outputDirectory.set(file("${build}/${rootProject.name}-${version}-doc"))
        suppressObviousFunctions.set(false)
    }

    dokkaSourceSets {
        named("main") {
            // 配置源代码链接（GitHub）
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl("https://github.com/zhibeigg/Orryx/tree/master/src/main/kotlin")
                remoteLineSuffix.set("#L")
            }
            // 添加外部文档链接（如 JDK）
            externalDocumentationLinks.register("jdk8") {
                url("https://docs.oracle.com/javase/8/docs/api/")
                packageListUrl("https://docs.oracle.com/javase/8/docs/api/package-list")
            }
        }
        configureEach {
            // 包含/排除包
            documentedVisibilities.set(
                setOf(
                    org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public,
                    org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Protected,
                    org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Internal,
                    org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Private,
                    org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Package
                )
            )
            skipDeprecated.set(true)
            reportUndocumented.set(false)

            analysisPlatform.set(org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform.JVM)
            jdkVersion.set(8)

            perPackageOption {
                matchingRegex.set(".*internal.*")
                suppress.set(true) // 隐藏 internal API
            }
        }
    }
}
