package org.gitee.orryx.module.experience

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerExpChangeEvent
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobExperienceEvents
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.files
import org.gitee.orryx.utils.job
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.util.unsafeLazy
import taboolib.common5.cfloat
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.util.ReloadAwareLazy

object ExperienceLoaderManager {

    private val experienceLoaderMap by unsafeLazy { hashMapOf<String, ExperienceLoader>() }
    private val syncExperience by ReloadAwareLazy(Orryx.config) { Orryx.config.getBoolean("SyncExperience", true) }

    internal fun getExperience(key: String): IExperience? {
        return experienceLoaderMap[key]
    }

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        experienceLoaderMap.clear()
        files("experiences", "default.yml") { file ->
            val configuration = Configuration.loadFromFile(file)
            experienceLoaderMap[configuration.name] = ExperienceLoader(configuration.name, configuration)
        }
        consoleMessage("&e┣&7Experiences loaded &e${experienceLoaderMap.size} &a√")
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun expChange(e: PlayerExpChangeEvent) {
        if (!syncExperience) return
        e.player.job {
            it.giveExperience(e.amount)
        }
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun orExpUp(e: OrryxPlayerJobExperienceEvents.Up) {
        submit {
            sync(e.player, e.job)
        }
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun orExpDown(e: OrryxPlayerJobExperienceEvents.Down) {
        submit {
            sync(e.player, e.job)
        }
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    private fun changeJob(e: OrryxPlayerJobChangeEvents.Post) {
        sync(e.player, e.job)
    }

    private fun sync(player: Player, job: IPlayerJob) {
        if (!syncExperience) return
        player.level = job.level
        if (job.maxExperienceOfLevel == 0) {
            player.exp = 0.0f
        } else {
            player.exp = (job.experienceOfLevel.cfloat / job.maxExperienceOfLevel.cfloat).coerceIn(0.0f, 1.0f)
        }
    }
}