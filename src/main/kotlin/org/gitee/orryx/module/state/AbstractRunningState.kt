package org.gitee.orryx.module.state

import org.bukkit.GameMode

abstract class AbstractRunningState(open val data: PlayerData): IRunningState {

    override fun hasNext(runningState: IRunningState): Boolean {
        return !(data.player.gameMode == GameMode.SPECTATOR || data.player.gameMode == GameMode.CREATIVE)
    }
}