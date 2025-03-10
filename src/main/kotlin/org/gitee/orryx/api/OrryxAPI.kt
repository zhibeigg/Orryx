package org.gitee.orryx.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.gitee.orryx.api.interfaces.IKeyAPI
import org.gitee.orryx.api.interfaces.IReloadAPI
import org.gitee.orryx.api.interfaces.ITimerAPI
import org.gitee.orryx.core.reload.ReloadAPI
import taboolib.expansion.AsyncDispatcher
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.kether.KetherScriptLoader

object OrryxAPI {

    @Config("config.yml")
    lateinit var config: ConfigFile
        private set

    val ketherScriptLoader by lazy { KetherScriptLoader() }

    internal val saveScope = CoroutineScope(AsyncDispatcher + SupervisorJob())

    val keyAPI: IKeyAPI
        get() = KeyAPI

    val reloadAPI: IReloadAPI
        get() = ReloadAPI

    val timerAPI: ITimerAPI
        get() = TimerAPI

}