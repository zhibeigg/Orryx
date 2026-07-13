package org.gitee.orryx.core.skill

import com.germ.germplugin.api.event.GermKeyUpEvent
import eos.moe.dragoncore.api.event.KeyReleaseEvent
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.station.pipe.PipeTask
import org.gitee.orryx.utils.Tuple2
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.keySetting
import priv.seventeen.artist.arcartx.event.client.ClientKeyReleaseEvent
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PressSkillManager {

    internal val pressTaskMap = ConcurrentHashMap<UUID, Tuple2<String, PipeTask>>()

    internal fun register(playerId: UUID, skillKey: String, task: PipeTask): Boolean {
        return pressTaskMap.putIfAbsent(playerId, Tuple2(skillKey, task)) == null
    }

    internal fun remove(playerId: UUID, taskId: UUID) {
        pressTaskMap.computeIfPresent(playerId) { _, pair ->
            pair.takeUnless { it.second.uuid == taskId }
        }
    }

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

    @Ghost
    @SubscribeEvent
    private fun clientKeyRelease(e: ClientKeyReleaseEvent) {
        check(e.player, e.keyName.uppercase())
    }

    @SubscribeEvent
    private fun death(e: PlayerDeathEvent) {
        pressTaskMap.remove(e.entity.uniqueId)?.second?.broke()
    }

    /**
     * 玩家退出时清理蓄力技能任务，防止内存泄漏
     */
    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        pressTaskMap.remove(e.player.uniqueId)?.second?.broke()
    }

    @Reload(2)
    private fun clearAll() {
        pressTaskMap.values.toList().forEach { it.second.broke() }
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