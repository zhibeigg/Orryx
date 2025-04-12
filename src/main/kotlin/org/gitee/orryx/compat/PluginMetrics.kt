package org.gitee.orryx.compat

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.core.station.stations.StationLoaderManager
import taboolib.common.LifeCycle
import taboolib.common.platform.Platform
import taboolib.module.metrics.Metrics
import taboolib.module.metrics.charts.AdvancedPie
import taboolib.module.metrics.charts.SingleLineChart
import taboolib.platform.BukkitPlugin
import taboolib.platform.bukkit.Parallel

object PluginMetrics {

    internal lateinit var metrics: Metrics
        private set

    @Parallel(runOn = LifeCycle.ENABLE)
    private fun init() {
        metrics = Metrics(24289, BukkitPlugin.getInstance().description.version, Platform.BUKKIT)
        metrics.addCustomChart(SingleLineChart("skills") {
            SkillLoaderManager.getSkills().size
        })
        metrics.addCustomChart(SingleLineChart("stations") {
            StationLoaderManager.getStationLoaders().size
        })
        metrics.addCustomChart(AdvancedPie("skill_types") {
            val map = HashMap<String, Int>()
            SkillLoaderManager.getSkills().forEach { (_, u) ->
                map[u.type] = (map[u.type] ?: 0) + 1
            }
            map
        })
    }

}