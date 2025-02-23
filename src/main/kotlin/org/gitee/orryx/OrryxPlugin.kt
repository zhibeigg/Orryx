package org.gitee.orryx

import org.gitee.orryx.core.skill.SkillLoaderManager
import taboolib.common.LifeCycle
import taboolib.common.PrimitiveIO
import taboolib.common.PrimitiveSettings.IS_ISOLATED_MODE
import taboolib.common.PrimitiveSettings.REPO_CENTRAL
import taboolib.common.classloader.IsolatedClassLoader
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common5.cbool
import taboolib.module.chat.colored
import taboolib.module.metrics.Metrics
import taboolib.module.metrics.charts.SingleLineChart
import taboolib.platform.BukkitPlugin


object OrryxPlugin : Plugin() {

    lateinit var metrics: Metrics
        private set

    /**
     * "com.github.ben-manes.caffeine:caffeine:2.9.3"
     *
     * "org.joml:joml:1.10.7"
     *
     * "com.larksuite.oapi:oapi-sdk:2.4.7"
     * */
    @Awake(LifeCycle.CONST)
    private fun load() {
        // 加载启动类
        try {
            val delegateClass = Class.forName("taboolib.common.PrimitiveLoader", true, IsolatedClassLoader.INSTANCE)
            val method = delegateClass.getDeclaredMethod("load", String::class.java, String::class.java, String::class.java, String::class.java, Boolean::class.java, Boolean::class.java, List::class.java)
            method.isAccessible = true
            val rule = ArrayList<Array<String>>()
            fun load(group: String, name: String, version: String) {
                if (method.invoke(null, REPO_CENTRAL, group, name, version, IS_ISOLATED_MODE, true, rule).cbool) {
                    PrimitiveIO.println(PrimitiveIO.t("加载外部依赖成功 {0}:{1}:{2}", "Load outside library success {0}:{1}:{2}"), group, name, version)
                } else {
                    PrimitiveIO.println(PrimitiveIO.t("加载外部依赖失败 {0}:{1}:{2}", "Load outside library failed {0}:{1}:{2}"), group, name, version)
                }
            }
            load("com.github.ben-manes.caffeine", "caffeine", "2.9.3")
            load("org.joml", "joml", "1.10.7")
            load("com.larksuite.oapi", "oapi-sdk", "2.4.7")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun onEnable() {
        metrics = Metrics(24289, BukkitPlugin.getInstance().description.version, Platform.BUKKIT)
        metrics.addCustomChart(SingleLineChart("skills") {
            SkillLoaderManager.getSkills().size
        })
        info("&e┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
        info("&e┃&a _____                                                  ".colored())
        info("&e┃&a/\\  __`\\                                              ".colored())
        info("&e┃&a\\ \\ \\/\\ \\  _ __   _ __   __  __   __  _            ".colored())
        info("&e┃&a \\ \\ \\ \\ \\/\\`'__\\/\\`'__\\/\\ \\/\\ \\ /\\ \\/'\\".colored())
        info("&e┃&a  \\ \\ \\_\\ \\ \\ \\/ \\ \\ \\/ \\ \\ \\_\\ \\\\/>  </".colored())
        info("&e┃&a   \\ \\_____\\ \\_\\  \\ \\_\\  \\/`____ \\/\\_/\\_\\  ".colored())
        info("&e┃&a    \\/_____/\\/_/   \\/_/   `/___/> \\//\\/_/          ".colored())
        info("&e┃&a                             /\\___/                    ".colored())
        info("&e┃&a                             \\/__/                     ".colored())
        info("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
    }

    override fun onDisable() {
        info("&eOrryx &a卸载".colored())
    }

}