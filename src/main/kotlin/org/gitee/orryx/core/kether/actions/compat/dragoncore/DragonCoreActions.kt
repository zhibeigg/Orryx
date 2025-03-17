package org.gitee.orryx.core.kether.actions.compat.dragoncore

import eos.moe.armourers.api.DragonAPI
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.util.parseUUID
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.*

object DragonCoreActions {

    private val armourersMap by lazy { hashMapOf<UUID, MutableList<String>>() }

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

    @KetherParser(["dragoncore", "dragon"], namespace = ORRYX_NAMESPACE, shared = true)
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
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("DragonCore附属语句", "发送暴雪粒子", "dragoncore", true)
                .description("发送暴雪粒子")
                .addEntry("粒子标识符", Type.SYMBOL, false, head = "effect/particle")
                .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
                .addEntry("粒子ID", Type.STRING, false)
                .addEntry("粒子文件名", Type.STRING, false)
                .addEntry("x,y,z或者实体UUID", Type.STRING, false)
                .addEntry("x,y,z旋转角度", Type.STRING, false, "0,0,0", head = "rotation")
                .addEntry("x,y,z平移位置", Type.VECTOR, false, "0,0,0", head = "translate")
                .addEntry("存活时长tick", Type.STRING, false, "100", head = "timeout")
                .addContainerEntry("可视玩家", true, "@server", "viewers"),
            Action.new("DragonCore附属语句", "移除暴雪粒子", "dragoncore", true)
                .description("移除暴雪粒子")
                .addEntry("粒子标识符", Type.SYMBOL, false, head = "effect/particle")
                .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
                .addEntry("粒子ID", Type.STRING, false)
                .addContainerEntry("可视玩家", true, "@server"),
            Action.new("DragonCore附属语句", "清除暴雪粒子", "dragoncore", true)
                .description("清除暴雪粒子")
                .addEntry("粒子标识符", Type.SYMBOL, false, head = "effect/particle")
                .addEntry("清除标识符", Type.SYMBOL, false, head = "clear")
                .addContainerEntry("可视玩家", true, "@self"),
            Action.new("DragonCore附属语句", "设置玩家动作", "dragoncore", true)
                .description("设置玩家动作")
                .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
                .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
                .addEntry("玩家标识符", Type.SYMBOL, false, head = "player")
                .addEntry("动作名", Type.STRING, false)
                .addEntry("动作速度", Type.FLOAT, false)
                .addContainerEntry("设置玩家", true, "@self"),
            Action.new("DragonCore附属语句", "移除玩家动作", "dragoncore", true)
                .description("移除玩家动作")
                .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
                .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
                .addEntry("玩家标识符", Type.SYMBOL, false, head = "player")
                .addContainerEntry("移除玩家", true, "@self"),
            Action.new("DragonCore附属语句", "设置实体动作", "dragoncore", true)
                .description("设置实体动作")
                .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
                .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
                .addEntry("实体标识符", Type.SYMBOL, false, head = "entity")
                .addEntry("动作名", Type.STRING, false)
                .addEntry("过渡时间", Type.INT, false)
                .addEntry("动作速度", Type.FLOAT, false)
                .addContainerEntry("设置实体", false)
                .addContainerEntry("可视玩家", true, "@server", "viewers"),
            Action.new("DragonCore附属语句", "移除实体动作", "dragoncore", true)
                .description("移除实体动作")
                .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
                .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
                .addEntry("实体标识符", Type.SYMBOL, false, head = "entity")
                .addEntry("动作名", Type.STRING, false)
                .addEntry("过渡时间", Type.INT, false)
                .addContainerEntry("设置实体", false)
                .addContainerEntry("可视玩家", true, "@server", "viewers"),
            Action.new("DragonCore附属语句", "设置玩家手持物品动作", "dragoncore", true)
                .description("设置玩家手持物品动作")
                .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
                .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
                .addEntry("物品标识符", Type.SYMBOL, false, head = "item")
                .addEntry("动作名", Type.STRING, false)
                .addEntry("动作速度", Type.FLOAT, false)
                .addContainerEntry("设置实体", true, "@self")
                .addContainerEntry("可视玩家", true, "@server", "viewers"),
            Action.new("DragonCore附属语句", "设置方块动作", "dragoncore", true)
                .description("设置方块动作")
                .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
                .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
                .addEntry("方块标识符", Type.SYMBOL, false, head = "block")
                .addEntry("动作名", Type.STRING, false)
                .addEntry("xyz位置", Type.VECTOR, false)
                .addContainerEntry("可视玩家", true, "@self"),
        )
    ) {
        it.switch {
            case("armourers") {
                when (it.expects("send", "clear")) {
                    "send" -> sendArmourers(it)
                    "clear" -> clearArmourers(it)
                    else -> error("龙核armourers书写错误")
                }
            }
            case("effect", "particle") {
                when (it.expects("send", "remove", "clear")) {
                    "send" -> sendEffect(it)
                    "remove" -> removeEffect(it)
                    "clear" -> clearEffect(it)
                    else -> error("龙核effect书写错误")
                }
            }
            case("animation", "ani") {
                when (it.expects("set", "remove")) {
                    "set" -> setAnimation(it)
                    "remove" -> removeAnimation(it)
                    else -> error("龙核animation书写错误")
                }
            }
            case("sound") {
                TODO()
            }
        }
    }

    private fun sendArmourers(reader: QuestReader): ScriptAction<Any?> {
        val armourers = reader.nextParsedAction()
        val timeout = reader.nextHeadAction("timeout", "100")
        val container = reader.nextTheyContainerOrNull()

        return actionNow {
            run(armourers).str { armourers ->
                run(timeout).long { timeout ->
                    containerOrSelf(container) { container ->
                        container.forEachInstance<PlayerTarget> { player ->
                            armourersMap.getOrPut(player.uniqueId) { mutableListOf() }.add(armourers)
                            DragonAPI.updatePlayerSkin(player.getSource())
                            SimpleTimeoutTask.createSimpleTask(timeout) {
                                if (armourersMap.containsKey(player.uniqueId)) {
                                    armourersMap[player.uniqueId]!!.remove(armourers)
                                    DragonAPI.updatePlayerSkin(player.getSource())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clearArmourers(reader: QuestReader): ScriptAction<Any?> {
        val container1 = reader.nextTheyContainerOrNull()
        val armourers = reader.nextParsedAction()
        val container2 = reader.nextTheyContainerOrNull()

        return actionNow {
            if (container1 == null) {
                run(armourers).str { armourers ->
                    containerOrSelf(container2) { container ->
                        container.forEachInstance<PlayerTarget> { player ->
                            if (armourersMap.containsKey(player.uniqueId)) {
                                armourersMap[player.uniqueId]!!.remove(armourers)
                                DragonAPI.updatePlayerSkin(player.getSource())
                            }
                        }
                    }
                }
            } else {
                containerOrSelf(container1) { container ->
                    container.forEachInstance<PlayerTarget> { player ->
                        if (armourersMap.containsKey(player.uniqueId)) {
                            armourersMap[player.uniqueId]!!.clear()
                            DragonAPI.updatePlayerSkin(player.getSource())
                        }
                    }
                }
            }
        }
    }

    private fun sendEffect(reader: QuestReader): ScriptAction<Any?> {
        val id = reader.nextParsedAction()
        val name = reader.nextParsedAction()
        val posOrEntityUUID = reader.nextParsedAction()
        val rotation = reader.nextHeadAction("rotation", "0,0,0")
        val translate = reader.nextHeadAction("translate", "")
        val lifeTime = reader.nextHeadAction("timeout", "100")
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(id).str { id ->
                run(name).str { name ->
                    run(posOrEntityUUID).str { posOrEntityUUID ->
                        run(rotation).str { rotation ->
                            run(translate).vector { translate ->
                                run(lifeTime).int { lifeTime ->
                                    container(viewers, serverPlayerContainer) {container ->
                                        container.forEachInstance<PlayerTarget> { player ->
                                            PacketSender.addParticle(player.getSource(), name, id, posOrEntityUUID, rotation, translate.dragonString(), lifeTime)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeEffect(reader: QuestReader): ScriptAction<Any?> {
        val id = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(id).str { id ->
                container(they, serverPlayerContainer) {
                    id.parseUUID()?.let { uuid ->
                        it.forEachInstance<PlayerTarget> { target ->
                            PacketSender.removeParticle(target.getSource(), uuid)
                        }
                    } ?: run {
                        it.forEachInstance<PlayerTarget> { target ->
                            PacketSender.removeParticle(target.getSource(), id)
                        }
                    }
                }
            }
        }
    }

    private fun clearEffect(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            containerOrSelf(they) {
                it.forEachInstance<PlayerTarget> { target ->
                    PacketSender.clearParticle(target.getSource())
                }
            }
        }
    }

    private fun setAnimation(reader: QuestReader): ScriptAction<Any?> {
        return when(reader.expects("player", "entity", "item", "block")) {
            "player" -> setPlayerAnimation(reader)
            "entity" -> setModelEntityAnimation(reader)
            "item" -> setItemAnimation(reader)
            "block" -> setBlockAnimation(reader)
            else -> error("龙核set animation类型错误")
        }
    }

    private fun removeAnimation(reader: QuestReader): ScriptAction<Any?> {
        return when(reader.expects("player", "entity")) {
            "player" -> removePlayerAnimation(reader)
            "entity" -> removeModelEntityAnimation(reader)
            else -> error("龙核remove animation类型错误")
        }
    }

    private fun setPlayerAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val speed = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(animation).str { animation ->
                run(speed).float { speed ->
                    containerOrSelf(players) { players ->
                        players.forEachInstance<PlayerTarget> { player ->
                            PacketSender.setPlayerAnimation(player.getSource(), animation, speed)
                        }
                    }
                }
            }
        }
    }

    private fun removePlayerAnimation(reader: QuestReader): ScriptAction<Any?> {
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            containerOrSelf(players) { players ->
                players.forEachInstance<PlayerTarget> { player ->
                    PacketSender.removePlayerAnimation(player.getSource())
                }
            }
        }
    }

    private fun setModelEntityAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val transitionTime = reader.nextParsedAction()
        val speed = reader.nextParsedAction()
        val entities = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(transitionTime).int { transitionTime ->
                    run(speed).float { speed ->
                        entities ?: error("龙核动作无设置实体")
                        containerOrSelf(entities) {entities ->
                            container(viewers, serverPlayerContainer) {viewers ->
                                val players = viewers.get<PlayerTarget>()
                                entities.forEachInstance<ITargetEntity<*>> { target ->
                                    if (target is PlayerTarget) return@forEachInstance
                                    players.forEach { viewer ->
                                        PacketSender.setModelEntityAnimation(viewer.getSource(), target.entity.uniqueId, animation, transitionTime*50, speed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeModelEntityAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val transitionTime = reader.nextParsedAction()
        val entities = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(transitionTime).int { transitionTime ->
                    entities ?: error("龙核动作无设置实体")
                    containerOrSelf(entities) { entities ->
                        container(viewers, serverPlayerContainer) { viewers ->
                            val players = viewers.get<PlayerTarget>()
                            entities.forEachInstance<ITargetEntity<*>> { target ->
                                if (target is PlayerTarget) return@forEachInstance
                                players.forEach { viewer ->
                                    PacketSender.removeModelEntityAnimation(viewer.getSource(), target.entity.uniqueId, animation, transitionTime*50)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setItemAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val speed = reader.nextParsedAction()
        val entities = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(speed).float { speed ->
                    containerOrSelf(entities) {entities ->
                        container(viewers, serverPlayerContainer) {viewers ->
                            val players = viewers.get<PlayerTarget>()
                            entities.forEachInstance<ITargetEntity<*>> { entity ->
                                players.forEach { player ->
                                    DragonCoreCustomPacketSender.setEntityModelItemAnimation(player.getSource(), entity.entity.uniqueId, animation, speed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setBlockAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val xyz = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(animation).str { animation ->
                run(xyz).vector { xyz ->
                    containerOrSelf(players) { players ->
                        players.forEachInstance<PlayerTarget> { player ->
                            PacketSender.setBlockAnimation(player.getSource(), xyz.x().toInt(), xyz.y().toInt(), xyz.z().toInt(), animation)
                        }
                    }
                }
            }
        }
    }

}