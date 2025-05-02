package org.gitee.orryx.module.state

import org.bukkit.GameMode
import org.gitee.orryx.module.spirit.ISpiritManager

abstract class AbstractRunningState(open val data: PlayerData): IRunningState {

    override fun hasNext(runningState: IRunningState): Boolean {
        if (data.player.gameMode == GameMode.SPECTATOR || data.player.gameMode == GameMode.CREATIVE) return false
        val state = runningState.state
        return if (state is ISpiritCost) {
            ISpiritManager.INSTANCE.haveSpirit(data.player, state.spirit)
        } else {
            true
        }
    }
}