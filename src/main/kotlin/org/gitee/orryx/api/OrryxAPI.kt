package org.gitee.orryx.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.gitee.orryx.api.interfaces.IKeyAPI
import org.gitee.orryx.api.interfaces.IOrryxAPI
import org.gitee.orryx.api.interfaces.IReloadAPI
import org.gitee.orryx.api.interfaces.ITimerAPI
import taboolib.common.platform.PlatformFactory
import taboolib.expansion.AsyncDispatcher
import taboolib.module.kether.KetherScriptLoader

class OrryxAPI: IOrryxAPI {

    override val keyAPI: IKeyAPI = PlatformFactory.getAPI<IKeyAPI>()

    override val reloadAPI: IReloadAPI = PlatformFactory.getAPI<IReloadAPI>()

    override val timerAPI: ITimerAPI = PlatformFactory.getAPI<ITimerAPI>()

    companion object {

        val ketherScriptLoader by lazy { KetherScriptLoader() }

        internal val saveScope = CoroutineScope(AsyncDispatcher + SupervisorJob())

    }

}