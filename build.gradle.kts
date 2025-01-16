
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
    id("org.gitee.orryxwiki") version "1.0.0"
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
            name("DragonArmourers").optional(true)
            name("DragonCore").optional(true)
            name("GlowAPI").optional(true)
            name("GDDTitle").optional(true)
            name("DungeonPlus").optional(true)
            name("Adyeshach").optional(true)
        }
    }
    relocate("com.google", "org.gitee.orryx.google")
    relocate("com.github.benmanes", "org.gitee.orryx.benmanes")
    version { taboolib = "6.2.1-df22fb1" }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.tabooproject.org/repository/releases") }
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

    compileOnly("com.gitee.redischannel:RedisChannel:1.1:api@jar")
    compileOnly("ink.ptms.adyeshach:all:2.0.0-snapshot-36")
    compileOnly("com.gitee:DragonCore:2.6.2.9")
    compileOnly("com.gitee:MythicMobs:4.11.0")
    compileOnly("com.gitee:DragonArmourers:6.72")
    compileOnly("com.gitee:GDDTitle:2.1")
    compileOnly("com.gitee:GlowAPI:1.4.6")
    compileOnly("com.gitee:DungeonPlus:1.3.9")

    taboo("com.google.code.gson:gson:2.10.1")
    taboo("com.github.ben-manes.caffeine:caffeine:2.9.3")

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