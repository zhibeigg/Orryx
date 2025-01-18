package org.gitee.orryx.core.kether.actions

import eos.moe.dragoncore.network.PacketSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.common.task.SimpleTimeoutTask.Companion.register
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.kether.*
import java.util.*
import kotlin.collections.set

object BuffActions {

    private val buffMap = mutableMapOf<String, Buff>()
    private val playerBuffMap = mutableMapOf<UUID, MutableMap<String, PlayerBuff>>()

    class Buff(val key: String, description: List<String>) {

        val description: (Player) -> List<String> = {
            getDescription(it, description)
        }

        private fun getDescription(player: Player, description: List<String>): List<String> {
            return player.parse(description, emptyMap())
        }

    }

    class PlayerBuff(val player: Player, val buff: Buff, val timeout: Long) : SimpleTimeoutTask(timeout) {

        override val closed: () -> Unit = {
            playerBuffMap[player.uniqueId]?.remove(buff.key)
        }

    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        playerBuffMap.remove(e.player.uniqueId)
    }

    @Config("buff.yml", migrate = true)
    lateinit var config: Configuration

    @Reload(2)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        config.reload()
        buffMap.clear()
        config.getKeys(false).forEach {
            config.getStringList("$it.Description").let { description ->
                buffMap[it] = Buff(it, description)
            }
        }
        info("&e┣&7Buffs loaded &e${buffMap.size} &a√".colored())
    }

    fun sendBuff(player: Player, name: String, timeout: Long) {
        val buff = buffMap[name] ?: return
        playerBuffMap.putIfAbsent(
            player.uniqueId,
            mutableMapOf(name to PlayerBuff(player, buff, timeout).register() as PlayerBuff)
        )
        if (DragonCoreEnabled) {
            sendBuffDragonCore(player, buff, timeout)
        }
    }

    fun clearBuff(player: Player, name: String) {
        playerBuffMap[player.uniqueId]?.get(name)?.closed?.invoke()
        if (DragonCoreEnabled) {
            clearBuffDragonCore(player, name)
        }
    }

    fun clearBuffAll(player: Player) {
        playerBuffMap[player.uniqueId]?.values?.toList()?.forEach {
            it.closed()
        }
        if (DragonCoreEnabled) {
            clearBuffDragonCoreAll(player)
        }
    }

    fun hasBuff(player: Player, buff: String): Boolean {
        return playerBuffMap[player.uniqueId]?.containsKey(buff) == true
    }

    //龙核
    private fun sendBuffDragonCore(player: Player, buff: Buff, timeout: Long) {
        val description = buff.description(player)
        PacketSender.sendSyncPlaceholder(
            player,
            mapOf("dragoncore_buff" to "${buff.key}<br>${timeout * 50}<br>${description.joinToString("\n")}")
        )
    }

    private fun clearBuffDragonCore(player: Player, buff: String) {
        PacketSender.sendSyncPlaceholder(
            player,
            mapOf("dragoncore_buff" to "$buff<br>0")
        )
    }

    private fun clearBuffDragonCoreAll(player: Player) {
        PacketSender.sendDeletePlaceholderCache(player, "dragoncore_buff", false)
    }

    @KetherParser(["buff"], namespace = NAMESPACE, shared = true)
    fun buff() = scriptParser(
        arrayOf(
            Action.new("Orryx Profile玩家信息", "设置状态效果", "buff", true)
                .description("设置玩家状态效果")
                .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
                .addEntry("buff名", Type.STRING, false)
                .addEntry("持续时长", Type.LONG, true)
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("Orryx Profile玩家信息", "清除状态效果", "buff", true)
                .description("清除玩家状态效果")
                .addEntry("清除标识符", Type.SYMBOL, false, head = "clear")
                .addEntry("buff名", Type.STRING, true, "ALL")
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("Orryx Profile玩家信息", "是否有状态效果", "buff", true)
                .description("检测玩家是否有状态效果")
                .addEntry("检测标识符", Type.SYMBOL, false, head = "has")
                .addEntry("buff名", Type.STRING, false)
                .addContainerEntry(optional = true, default = "@self")
        )
    ) {
        it.switch {
            case("send") {
                val buff = it.nextParsedAction()
                val timeout = it.nextParsedAction()
                val container = it.nextTheyContainer()
                actionNow {
                    run(buff).str { buff ->
                        run(timeout).long { timeout ->
                            container.readContainer(script()).orElse(self()).forEachInstance<PlayerTarget> { player ->
                                sendBuff(player.player, buff, timeout)
                            }
                        }
                    }
                }
            }
            case("clear") {
                val container1 = it.nextTheyContainer()
                val buff = it.nextParsedAction()
                val container2 = it.nextTheyContainer()
                actionNow {
                    if (container1 == null) {
                        run(buff).str { buff ->
                            container2.readContainer(script()).orElse(self()).forEachInstance<PlayerTarget> { player ->
                                clearBuff(player.player, buff)
                            }
                        }
                    } else {
                        container1.readContainer(script()).orElse(self()).forEachInstance<PlayerTarget> { player ->
                            clearBuffAll(player.player)
                        }
                    }
                }
            }
            case("has") {
                val buff = it.nextParsedAction()
                val container = it.nextTheyContainer()
                actionFuture {
                    run(buff).str { buff ->
                        container.readContainer(script()).orElse(self()).forEachInstance<PlayerTarget> { player ->
                            it.complete(hasBuff(player.player, buff))
                        }
                    }
                }
            }
        }
    }

}

