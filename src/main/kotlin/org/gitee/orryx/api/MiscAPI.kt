package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.IMiscAPI
import org.gitee.orryx.module.experience.ExperienceLoaderManager
import org.gitee.orryx.module.experience.IExperience
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

class MiscAPI: IMiscAPI {

    override fun getExperience(key: String): IExperience? {
        return ExperienceLoaderManager.getExperience(key)
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IMiscAPI>(MiscAPI())
        }
    }
}