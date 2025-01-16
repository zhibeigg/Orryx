rootProject.name = "Orryx"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("http://play.mcwar.cn:18888/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
    }
}