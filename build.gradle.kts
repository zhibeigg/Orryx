import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val publishUsername: String by project
val publishPassword: String by project
val build: String by project

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("io.izzel.taboolib") version "2.0.23"
    id("org.jetbrains.dokka") version "2.0.0"
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
        //repoTabooLib = "https://www.mcwar.cn/nexus/repository/maven-public/"
    }
    description {
        name = "Orryx"
        desc("跨时代技能插件，支持实现复杂逻辑，为稳定高效而生")
        contributors {
            name("zhibei")
        }
        links {
            name("homepage").url("https://www.mcwar.cn/orryx")
        }
        dependencies {
            name("Adyeshach").optional(true)
            name("DragonCore").optional(true)
            name("DragonArmourers").optional(true)
            name("GermPlugin").optional(true)
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
        }
    }
    relocate("com.github.benmanes.caffeine", "org.gitee.orryx.caffeine")
    relocate("org.joml", "org.gitee.orryx.joml")
    relocate("com.larksuite.oapi", "org.gitee.orryx.larksuite.oapi")
    relocate("com.eatthepath.uuid", "org.gitee.orryx.eatthepath.uuid")
    relocate("kotlinx.serialization", "org.gitee.orryx.serialization")
    version { taboolib = "6.2.3-b217935" }
}

repositories {
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases")
    maven("https://www.mcwar.cn/nexus/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("org.gitee.nodens:Nodens:1.10.14:api")
    compileOnly("com.gitee.redischannel:RedisChannel:1.10.6:api")
    compileOnly("ink.ptms.adyeshach:plugin:2.0.26:api")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("org.eldergod.ext:DragonCore:2.6.2.9")
    compileOnly("org.eldergod.ext:GermPlugin:4.4.1-5")
    compileOnly("org.eldergod.ext:DragonArmourers:6.72")
    implementation("org.eldergod.ext:MythicMobs:4.11.0") {
        exclude("com.google.guava", "guava")
    }
    compileOnly("org.eldergod.ext:GDDTitle:2.1")
    compileOnly("org.eldergod.ext:GlowAPI:1.4.6")
    compileOnly("org.eldergod.ext:AttributePlus:api")
    compileOnly("org.eldergod.ext:DungeonPlus:1.4.3")
    compileOnly("org.eldergod.ext:ProtocolLib:5.3.0")

    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    compileOnly("org.joml:joml:1.10.7")
    compileOnly("com.larksuite.oapi:oapi-sdk:2.4.22")
    compileOnly("com.eatthepath:fast-uuid:0.2.0")

    testCompileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    testCompileOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    testCompileOnly("com.eatthepath:fast-uuid:0.2.0")

    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    destinationDirectory.set(File(build))
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.set(listOf("-Xjvm-default=all"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://www.mcwar.cn/nexus/repository/maven-releases/")
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
        }
    }
}

tasks.dokkaHtml {
    // 配置输出目录
    outputDirectory.set(file("${build}/${rootProject.name}-${version}-doc"))
    // 配置模块名称
    moduleName.set("Orryx")
    // 禁用自动生成文档链接
    suppressObviousFunctions.set(false)
    dokkaSourceSets {
        named("main") {
            // 配置源代码链接（GitHub）
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(uri("https://github.com/zhibeigg/Orryx/tree/master/src/main/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
            // 添加外部文档链接（如 JDK）
            externalDocumentationLink {
                url.set(uri("https://docs.oracle.com/javase/8/docs/api/").toURL())
                packageListUrl.set(uri("https://docs.oracle.com/javase/8/docs/api/package-list").toURL())
            }
        }
        configureEach {
            // 包含/排除包
            includeNonPublic.set(true)
            skipDeprecated.set(true)
            reportUndocumented.set(true)

            platform.set(org.jetbrains.dokka.Platform.jvm)
            jdkVersion.set(8)

            perPackageOption {
                matchingRegex.set(".*internal.*")
                suppress.set(true) // 隐藏 internal API
            }
        }
    }
}
