package org.gitee.orryx.core.experience

import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.files
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration

object ExperienceLoaderManager {

    private val experienceLoaderMap by unsafeLazy { hashMapOf<String, ExperienceLoader>() }

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
        info("&e┣&7Experiences loaded &e${experienceLoaderMap.size} &a√".colored())
    }

}