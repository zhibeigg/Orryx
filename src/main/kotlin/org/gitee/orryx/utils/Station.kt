package org.gitee.orryx.utils

import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.station.stations.IStation
import taboolib.common.platform.ProxyCommandSender
import taboolib.common5.clong
import taboolib.module.kether.orNull


internal fun IStation.getBaffle(sender: ProxyCommandSender, stationParameter: StationParameter): Long {
    baffleAction ?: return 0
    return ScriptManager.runScript(sender, stationParameter, baffleAction!!).orNull().clong * 50
}