package org.gitee.orryx.core.skill

import com.germ.germplugin.api.event.GermKeyUpEvent
import eos.moe.dragoncore.api.event.KeyReleaseEvent
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.station.pipe.PipeTask
import org.gitee.orryx.utils.Tuple2
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.keySetting
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object PressSkillManager {

    internal val pressTaskMap = ConcurrentHashMap<UUID, Tuple2<String, PipeTask>>()

    @Ghost
    @SubscribeEvent
    private fun keyRelease(e: KeyReleaseEvent) {
        check(e.player, e.key.uppercase())
    }

    @Ghost
    @SubscribeEvent
    private fun keyUp(e: GermKeyUpEvent) {
        check(e.player, e.keyType.simpleKey)
    }

    @SubscribeEvent
    private fun death(e: PlayerDeathEvent) {
        pressTaskMap.remove(e.entity.uniqueId)?.second?.close { CompletableFuture.completedFuture(null) }
    }

    @Reload(2)
    private fun clearAll() {
        pressTaskMap.forEach {
            it.value.second.close { CompletableFuture.completedFuture(null) }
        }
        pressTaskMap.clear()
    }

    private fun check(player: Player, key: String) {
        val pair = pressTaskMap[player.uniqueId] ?: return
        player.job {
            val group = BindKeyLoaderManager.getGroup(it.group) ?: return@job
            val map = it.bindKeyOfGroup[group] ?: return@job
            val keybind = map.firstNotNullOfOrNull { entry ->
                if (entry.value == pair.first) {
                    entry.key
                } else {
                    null
                }
            } ?: return@job
            player.keySetting { keySetting ->
                val mapping = keySetting.bindKeyMap[keybind] ?: return@keySetting
                if (mapping.split("+").contains(key)) {
                    pair.second.complete()
                }
            }
        }
    }
}