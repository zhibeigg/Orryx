package org.gitee.orryx.api.interfaces

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import taboolib.expansion.AsyncDispatcher
import taboolib.module.kether.KetherScriptLoader

interface IOrryxAPI {

    val keyAPI: IKeyAPI

    val reloadAPI: IReloadAPI

    val timerAPI: ITimerAPI

}