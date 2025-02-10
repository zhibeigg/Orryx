package org.gitee.orryx.core.kether.actions.compat.dragoncore

import eos.moe.armourers.api.DragonAPI
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.kether.*
import java.util.*

object DragonCoreActions {

    private val armourersMap = mutableMapOf<UUID, MutableList<String>>()

    @Ghost
    @SubscribeEvent
    private fun armourers(e: PlayerSkinUpdateEvent) {
        val list = armourersMap[e.player.uniqueId]
        if (!list.isNullOrEmpty()) {
            e.skinList.addAll(list)
        }
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        armourersMap.remove(e.player.uniqueId)
    }

    @KetherParser(["dragoncore", "dragon"], namespace = NAMESPACE, shared = true)
    private fun dragonCore() = scriptParser(
        arrayOf(
            Action.new("DragonCore附属语句", "设置临时时装", "dragoncore", true)
                .description("设置临时时装")
                .addEntry("armourers标识符", Type.SYMBOL, false, head = "armourers")
                .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
                .addEntry("时装名", Type.STRING, false)
                .addEntry("临时时长", Type.LONG, true, "100", "timeout")
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("DragonCore附属语句", "清除临时时装", "dragoncore", true)
                .description("清除临时时装")
                .addEntry("armourers标识符", Type.SYMBOL, false, head = "armourers")
                .addEntry("清除标识符", Type.SYMBOL, false, head = "clear")
                .addEntry("时装名", Type.STRING, true, "ALL")
                .addContainerEntry(optional = true, default = "@self")
        )
    ) {
        it.switch {
            case("armourers") {
                when (it.expects("send", "clear")) {
                    "send" -> {
                        val armourers = it.nextParsedAction()
                        val timeout = it.nextHeadAction("timeout", "100")
                        val container = it.nextTheyContainer()

                        actionNow {
                            run(armourers).str { armourers ->
                                run(timeout).long { timeout ->
                                    containerOrSelf(container) { container ->
                                        container.forEachInstance<PlayerTarget> { player ->
                                            armourersMap.computeIfAbsent(player.uniqueId) { mutableListOf() }.add(armourers)
                                            DragonAPI.updatePlayerSkin(player.player)
                                            SimpleTimeoutTask.createSimpleTask(timeout) {
                                                if (armourersMap.containsKey(player.uniqueId)) {
                                                    armourersMap[player.uniqueId]!!.remove(armourers)
                                                    DragonAPI.updatePlayerSkin(player.player)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "clear" -> {
                        val container1 = it.nextTheyContainer()
                        val armourers = it.nextParsedAction()
                        val container2 = it.nextTheyContainer()

                        actionNow {
                            if (container1 == null) {
                                run(armourers).str { armourers ->
                                    containerOrSelf(container2) { container ->
                                        container.forEachInstance<PlayerTarget> { player ->
                                            if (armourersMap.containsKey(player.uniqueId)) {
                                                armourersMap[player.uniqueId]!!.remove(armourers)
                                                DragonAPI.updatePlayerSkin(player.player)
                                            }
                                        }
                                    }
                                }
                            } else {
                                containerOrSelf(container1) { container ->
                                    container.forEachInstance<PlayerTarget> { player ->
                                        if (armourersMap.containsKey(player.uniqueId)) {
                                            armourersMap[player.uniqueId]!!.clear()
                                            DragonAPI.updatePlayerSkin(player.player)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> error("out of case")
                }
            }
        }
    }


}