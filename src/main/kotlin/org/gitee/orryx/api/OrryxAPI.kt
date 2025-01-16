package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.IKeyAPI
import org.gitee.orryx.api.interfaces.IReloadAPI
import org.gitee.orryx.api.interfaces.ITimerAPI
import org.gitee.orryx.core.reload.ReloadAPI
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.kether.KetherScriptLoader

object OrryxAPI {

    @Config("config.yml")
    lateinit var config: Configuration

    val ketherScriptLoader by lazy { KetherScriptLoader() }

    val keyAPI: IKeyAPI
        get() = KeyAPI

    val reloadAPI: IReloadAPI
        get() = ReloadAPI

    val timerAPI: ITimerAPI
        get() = TimerAPI

}