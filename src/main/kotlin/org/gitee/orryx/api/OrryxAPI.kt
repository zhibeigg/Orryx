package org.gitee.orryx.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.gitee.orryx.api.interfaces.IKeyAPI
import org.gitee.orryx.api.interfaces.IReloadAPI
import org.gitee.orryx.api.interfaces.ITimerAPI
import org.gitee.orryx.core.reload.ReloadAPI
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.expansion.AsyncDispatcher
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.kether.KetherScriptLoader

@RuntimeDependencies(
    RuntimeDependency(
        "!com.github.ben-manes.caffeine:caffeine:2.9.3",
        test = "!org.gitee.orryx.caffeine.cache.Caffeine",
        relocate = ["!com.github.benmanes.caffeine", "!org.gitee.orryx.caffeine"],
        transitive = false
    ),
    RuntimeDependency(
        "!org.joml:joml:1.10.7",
        test = "!org.gitee.orryx.joml.Math",
        relocate = ["!org.joml", "!org.gitee.orryx.joml"],
        transitive = false
    ),
    RuntimeDependency(
        "!com.larksuite.oapi:oapi-sdk:2.4.7",
        test = "!org.gitee.orryx.larksuite.oapi.Client",
        relocate = ["!com.larksuite.oapi", "!org.gitee.orryx.larksuite.oapi"],
        transitive = false
    )
)
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