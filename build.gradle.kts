import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val publishUsername: String by project
val publishPassword: String by project
val build: String by project

plugins {
    java
    `maven-publish`
    id("io.izzel.taboolib") version "2.0.22"
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
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
    }
    description {
        name = "Orryx"
        desc("跨时代技能插件，支持实现复杂逻辑，为稳定高效而生")
        contributors {
            name("zhibei")
        }
        dependencies {
            name("RedisChannel").optional(true)
            name("MythicMobs").optional(true)
            name("PlaceholderAPI").optional(true)
            name("ProtocolLib").optional(true)
            name("packetevents").optional(true)
            name("DragonArmourers").optional(true)
            name("DragonCore").optional(true)
            name("GlowAPI").optional(true)
            name("GDDTitle").optional(true)
            name("DungeonPlus").optional(true)
            name("Adyeshach").optional(true)
            name("OriginAttribute").optional(true)
            name("AttributePlus").optional(true)
        }
    }
    version { taboolib = "6.2.2" }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.tabooproject.org/repository/releases") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven {
        url = uri("http://play.mcwar.cn:18888/repository/maven-public/")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("com.gitee.redischannel:RedisChannel:1.1:api")
    compileOnly("ink.ptms.adyeshach:all:2.0.0-snapshot-36")
    compileOnly("com.gitee:DragonCore:2.6.2.9")
    compileOnly("com.germ:germplugin:4.4.1-5")
    compileOnly("com.gitee:MythicMobs:4.11.0")
    compileOnly("com.gitee:DragonArmourers:6.72")
    compileOnly("com.gitee:GDDTitle:2.1")
    compileOnly("com.gitee:GlowAPI:1.4.6")
    compileOnly("com.gitee:DungeonPlus:1.3.9")
    compileOnly("ac.github.oa:OriginAttribute:1.1.4")
    compileOnly("org.serverct:ersha:3.3.3.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")

    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    compileOnly("org.joml:joml:1.10.7")
    compileOnly("com.larksuite.oapi:oapi-sdk:2.4.7")

    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    destinationDirectory.set(File(build))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
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
            url = uri("http://play.mcwar.cn:18888/repository/maven-releases/")
            isAllowInsecureProtocol = true
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

tasks.dokkaHtml.configure {
    // 导出的文档目录路径
    outputDirectory.set(File("${build}/doc"))
    dokkaSourceSets {
        named("main") {
            configureEach {
                platform.set(org.jetbrains.dokka.Platform.jvm)
                jdkVersion.set(8)
            }
        }
    }
}