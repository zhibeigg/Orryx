import io.izzel.taboolib.gradle.*

val publishUsername: String by project
val publishPassword: String by project
val build: String by project
val token: String by project

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("io.izzel.taboolib") version "2.0.27"
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
        //repoTabooLib = "https://jfrog.mcwar.cn/artifactory/maven-releases"
    }
    description {
        name = "Orryx"
        desc("跨时代技能插件，支持实现复杂逻辑，为稳定高效而生")
        contributors {
            name("zhibei")
        }
        links {
            name("homepage").url("https://orryx.mcwar.cn/")
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
            name("CloudPick").optional(true)
            name("ArcartX").optional(true)
        }
    }
    relocate("com.github.benmanes.caffeine", "org.gitee.orryx.caffeine")
    relocate("org.joml", "org.gitee.orryx.joml")
    relocate("com.larksuite.oapi", "org.gitee.orryx.larksuite.oapi")
    relocate("com.eatthepath.uuid", "org.gitee.orryx.eatthepath.uuid")
    relocate("kotlinx.serialization", "org.gitee.orryx.serialization")
    relocate("kotlin.script.experimental", "kotlin2120.script.experimental")
    version { taboolib = "6.2.4-1645904" }
}

repositories {
    mavenCentral()
    maven("https://jfrog.mcwar.cn/artifactory/maven-releases")
    maven("https://repo.codemc.io/repository/maven-releases")
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("org.gitee.nodens:Nodens:latest.release:api")
    compileOnly("com.gitee.redischannel:RedisChannel:latest.release:api")
    compileOnly("ink.ptms.adyeshach:api:latest.release")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("org.eldergod.ext:DragonCore:2.6.2.9")
    compileOnly("org.eldergod.ext:GermPlugin:4.4.1-5")
    compileOnly("org.eldergod.ext:DragonArmourers:6.72")
    compileOnly("org.eldergod.ext:MythicMobs:4.11.0") {
        exclude("com.google.guava", "guava")
    }
    compileOnly("org.eldergod.ext:GDDTitle:2.1")
    compileOnly("org.eldergod.ext:GlowAPI:1.4.6")
    compileOnly("org.eldergod.ext:AttributePlus:api")
    compileOnly("org.eldergod.ext:DungeonPlus:1.4.3")
    compileOnly("org.eldergod.ext:ProtocolLib:5.3.0")
    compileOnly("org.eldergod.ext:cloudpick:1.2.1:CloudPick")

    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    compileOnly("org.joml:joml:1.10.7")
    compileOnly("com.larksuite.oapi:oapi-sdk:2.4.22")
    compileOnly("com.eatthepath:fast-uuid:0.2.0")

    compileOnly("org.jetbrains.kotlin:kotlin-scripting-common:2.1.20")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-jvm:2.1.20")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.1.20")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:2.1.20")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-dependencies:2.1.20")
    compileOnly("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:2.1.20")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")

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
            url = uri("https://jfrog.mcwar.cn/artifactory/maven-releases")
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
