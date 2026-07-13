package org.gitee.orryx.module

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.spirit.ISpiritManager
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.util.onlinePlayers

/** Mana 与 Spirit 共用一个主线程 ticker，同一周期只遍历一次在线玩家。 */
object ResourceRegainTicker {

    private var task: PlatformExecutor.PlatformTask? = null
    private var manaPeriod = 20L
    private var spiritPeriod = 20L
    private var manaCountdown = 20L
    private var spiritCountdown = 20L

    @Reload(2)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        manaPeriod = Orryx.config.getLong("ManaRegainTick", 20L).coerceAtLeast(1L)
        spiritPeriod = Orryx.config.getLong("SpiritRegainTick", 20L).coerceAtLeast(1L)
        manaCountdown = manaPeriod
        spiritCountdown = spiritPeriod
        if (task == null) {
            task = submit(period = 1L) { tick() }
        }
    }

    private fun tick() {
        manaCountdown--
        spiritCountdown--
        val regainMana = manaCountdown <= 0L
        val regainSpirit = spiritCountdown <= 0L
        if (!regainMana && !regainSpirit) return
        if (regainMana) manaCountdown = manaPeriod
        if (regainSpirit) spiritCountdown = spiritPeriod

        onlinePlayers.forEach { player ->
            if (regainMana) {
                IManaManager.INSTANCE.regainMana(player).exceptionally { it.printStackTrace(); 0.0 }
            }
            if (regainSpirit) {
                ISpiritManager.INSTANCE.regainSpirit(player).exceptionally { it.printStackTrace(); 0.0 }
            }
        }
    }

    fun close() {
        task?.cancel()
        task = null
    }
}
