package org.gitee.orryx.core.kether.actions.compat.dragoncore

import eos.moe.armourers.api.DragonAPI
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import eos.moe.dragoncore.api.SlotAPI
import eos.moe.dragoncore.database.IDataBase
import eos.moe.dragoncore.network.PacketSender
import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.entity.EntityTypes
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common5.cfloat
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.CompletableFuture

object DragonCoreActions {

    private val armourersMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { hashMapOf<UUID, MutableList<String>>() }
    private val effectMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { hashMapOf<UUID, MutableList<ModelEffect>>() }

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
        Action.new("DragonCore附属语句", "更新时装", "dragoncore", true)
            .description("更新时装")
            .addEntry("armourers标识符", Type.SYMBOL, false, head = "armourers")
            .addEntry("更新标识符", Type.SYMBOL, false, head = "update")
            .addContainerEntry(optional = true, default = "@self"),
        Action.new("DragonCore附属语句", "发送暴雪粒子", "dragoncore", true)
            .description("发送暴雪粒子")
            .addEntry("粒子标识符", Type.SYMBOL, false, head = "effect/particle")
            .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
            .addEntry("粒子ID", Type.STRING, false)
            .addEntry("粒子文件名", Type.STRING, false)
            .addEntry("x,y,z旋转角度", Type.STRING, false, "0,0,0", head = "rotation")
            .addEntry("x,y,z平移位置", Type.VECTOR, false, "0,0,0", head = "translate")
            .addEntry("存活时长tick", Type.INT, false, "100", head = "timeout")
            .addContainerEntry("可视玩家", true, "@server", "viewers")
            .addContainerEntry("生成位置或者绑定实体", true, "@self"),
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
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
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
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
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
            .addContainerEntry("可视玩家", true, "@self", "viewers"),
        Action.new("DragonCore附属语句", "播放音乐", "dragoncore", true)
            .description("播放音乐")
            .addEntry("音乐标识符", Type.SYMBOL, false, head = "sound")
            .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
            .addEntry("音乐唯一ID", Type.STRING, false)
            .addEntry("音乐文件位置", Type.STRING, false)
            .addEntry("播放类型", Type.STRING, false)
            .addEntry("播放世界位置向量", Type.VECTOR, true, "可听玩家眼睛位置", "loc")
            .addEntry("是否循环", Type.BOOLEAN, true, head = "loop", default = "false")
            .addEntry("声音大小", Type.FLOAT, true, head = "by/with", default = "1.0")
            .addEntry("声音音调", Type.FLOAT, true, default = "1.0")
            .addContainerEntry("可听玩家", true, "@self"),
        Action.new("DragonCore附属语句", "停止播放音乐", "dragoncore", true)
            .description("停止播放音乐")
            .addEntry("音乐唯一ID", Type.STRING, false)
            .addContainerEntry("可听玩家", true, "@self"),
        Action.new("DragonCore附属语句", "运行龙核GUI方法", "dragoncore", true)
            .description("运行龙核GUI方法")
            .addEntry("方法标识符", Type.SYMBOL, false, head = "function/func")
            .addEntry("gui标识符", Type.SYMBOL, false, head = "gui")
            .addEntry("gui名字", Type.STRING, false)
            .addEntry("方法语句", Type.STRING, false)
            .addEntry("是否异步执行", Type.BOOLEAN, false)
            .addContainerEntry("客户端参与执行的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "运行龙核动作控制器方法", "dragoncore", true)
            .description("运行龙核动作控制器方法")
            .addEntry("方法标识符", Type.SYMBOL, false, head = "function/func")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("执行实体UUID", Type.STRING, false)
            .addEntry("方法语句", Type.STRING, false)
            .addContainerEntry("客户端参与执行的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "运行龙核headTag方法", "dragoncore", true)
            .description("运行龙核headTag方法")
            .addEntry("方法标识符", Type.SYMBOL, false, head = "function/func")
            .addEntry("tag标识符", Type.SYMBOL, false, head = "headtag/tag")
            .addEntry("执行实体UUID", Type.STRING, false)
            .addEntry("方法语句", Type.STRING, false)
            .addContainerEntry("客户端参与执行的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "打开GUI", "dragoncore", true)
            .description("打开龙核Gui")
            .addEntry("gui标识符", Type.SYMBOL, false, head = "gui")
            .addEntry("gui名字", Type.STRING, false)
            .addContainerEntry("打开GUI的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "打开HUD", "dragoncore", true)
            .description("打开龙核HUD")
            .addEntry("hud标识符", Type.SYMBOL, false, head = "hud")
            .addEntry("hud名字", Type.STRING, false)
            .addContainerEntry("打开HUD的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "发送同步papi数据", "dragoncore", true)
            .description("发送同步placeholder数据")
            .addEntry("placeholder标识符", Type.SYMBOL, false, head = "placeholders/papi")
            .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
            .addEntry("存储了的数据的键，用逗号隔开", Type.STRING, false)
            .addContainerEntry("发送数据的玩家", true, "@self")
            .example("dragon papi send a,b,c they \"@self\""),
        Action.new("DragonCore附属语句", "删除papi数据", "dragoncore", true)
            .description("删除客户端placeholder数据")
            .addEntry("placeholder标识符", Type.SYMBOL, false, head = "placeholders/papi")
            .addEntry("删除标识符", Type.SYMBOL, false, head = "delete/remove")
            .addEntry("删除的键", Type.STRING, false)
            .addEntry("是否检测startWith键", Type.BOOLEAN, false)
            .addContainerEntry("删除数据的玩家", true, "@self")
            .example("dragon papi delete a,b,c they \"@self\""),
        Action.new("DragonCore附属语句", "设置headTag", "dragoncore", true)
            .description("设置实体的headTag")
            .addEntry("headTag标识符", Type.SYMBOL, false, head = "headtag/tag")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
            .addEntry("设置的实体uuid", Type.STRING, false)
            .addEntry("匹配名", Type.STRING, false)
            .addContainerEntry("可视玩家", true, "@self", "viewers"),
        Action.new("DragonCore附属语句", "移除headTag", "dragoncore", true)
            .description("移除实体的headTag")
            .addEntry("headTag标识符", Type.SYMBOL, false, head = "headtag/tag")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("移除的实体uuid", Type.STRING, false)
            .addContainerEntry("可视玩家", true, "@self", "viewers"),
        Action.new("DragonCore附属语句", "设置实体模型", "dragoncore", true)
            .description("设置实体模型")
            .addEntry("模型标识符", Type.SYMBOL, false, head = "model")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
            .addEntry("设置的实体uuid", Type.STRING, false)
            .addEntry("匹配名", Type.STRING, false)
            .addContainerEntry("可视玩家", true, "@self", "viewers"),
        Action.new("DragonCore附属语句", "移除实体模型", "dragoncore", true)
            .description("移除实体模型")
            .addEntry("模型标识符", Type.SYMBOL, false, head = "model")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("移除的实体uuid", Type.STRING, false)
            .addContainerEntry("可视玩家", true, "@self", "viewers"),
        Action.new("DragonCore附属语句", "设置视角", "dragoncore", true)
            .description("设置第几人称视角")
            .addEntry("视角标识符", Type.SYMBOL, false, head = "view")
            .addEntry("视角(1,2,3)", Type.INT, false)
            .addContainerEntry("设置人称的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "设置windows窗口标题", "dragoncore", true)
            .description("设置windows窗口标题")
            .addEntry("窗口title标识符", Type.SYMBOL, false, head = "title")
            .addEntry("标题", Type.STRING, false)
            .addContainerEntry("设置标题的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "虚拟绑定实体位置", "dragoncore", true)
            .description("虚拟绑定实体位置")
            .addEntry("绑定标识符", Type.SYMBOL, false, head = "bindEntity/bind")
            .addEntry("被绑定的实体UUID", Type.STRING, false)
            .addEntry("绑定到的实体UUID", Type.STRING, false)
            .addEntry("偏移向量", Type.VECTOR, false)
            .addEntry("是否绑定yaw角", Type.BOOLEAN, false)
            .addEntry("是否绑定pitch角", Type.BOOLEAN, false)
            .addContainerEntry("可视玩家", true, "@self", "viewers"),
        Action.new("DragonCore附属语句", "实体模型特效绑定", "dragoncore", true)
            .description("实体模型特效绑定，脚本运行时间必须大于延迟消失时间，若提前停止将会直接回收实体")
            .addEntry("实体模型标识符", Type.SYMBOL, false, head = "modelEffect")
            .addEntry("创建标识符", Type.SYMBOL, false, head = "create")
            .addEntry("实体唯一ID", Type.STRING, false)
            .addEntry("模型匹配名", Type.STRING, false)
            .addEntry("延迟消失时间", Type.LONG, false)
            .addContainerEntry("绑定实体", true, "@self")
            .result("绑定的实体容器", Type.CONTAINER),
        Action.new("DragonCore附属语句", "实体模型特效移除", "dragoncore", true)
            .description("实体模型特效移除")
            .addEntry("实体模型标识符", Type.SYMBOL, false, head = "modelEffect")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("实体唯一ID", Type.STRING, false)
            .addContainerEntry("绑定的实体", true, "@self"),
        Action.new("DragonCore附属语句", "隐藏玩家手持武器", "dragoncore", true)
            .description("隐藏玩家手持武器")
            .addEntry("隐藏手持标识符", Type.SYMBOL, false, head = "invisibleHand")
            .addEntry("隐藏时间 0 为取消", Type.LONG, false)
            .addContainerEntry("取消的玩家", true, "@self"),
        Action.new("DragonCore附属语句", "获取槽位内物品", "dragoncore", true)
            .description("获取槽位内物品")
            .addEntry("槽位标识符", Type.SYMBOL, false, head = "slot")
            .addEntry("槽位名", Type.STRING, false)
            .addContainerEntry("获取的玩家", true, "@self")
    ) {
        it.switch {
            case("armourers") {
                when (it.expects("send", "clear", "update")) {
                    "send" -> sendArmourers(it)
                    "clear" -> clearArmourers(it)
                    "update" -> updateArmourers(it)
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
                when (it.expects("set", "to", "remove")) {
                    "set", "to" -> setAnimation(it)
                    "remove" -> removeAnimation(it)
                    else -> error("龙核animation书写错误")
                }
            }
            case("sound") {
                when (it.expects("send", "stop")) {
                    "send" -> sendSound(it)
                    "stop" -> stopSound(it)
                    else -> error("龙核sound书写错误")
                }
            }
            case("function", "func") {
                when (it.expects("gui", "animation", "ani", "headtag", "tag")) {
                    "gui" -> sendFunction(it)
                    "animation", "ani" -> sendAnimationFunction(it)
                    "headtag", "tag" -> sendTagFunction(it)
                    else -> error("龙核function书写错误")
                }
            }
            case("gui") { openGui(it) }
            case("hud") { openHud(it) }
            case("papi", "placeholders") {
                when (it.expects("send", "delete", "remove")) {
                    "send" -> sendSyncPlaceholder(it)
                    "delete", "remove" -> deletePlaceholderCache(it)
                    else -> error("龙核placeholder书写错误")
                }
            }
            case("headtag", "tag") {
                when (it.expects("set", "to", "remove")) {
                    "set", "to" -> setHeadTag(it)
                    "remove" -> removeHeadTag(it)
                    else -> error("龙核headtag书写错误")
                }
            }
            case("model") {
                when (it.expects("set", "to", "remove")) {
                    "set", "to" -> setEntityModel(it)
                    "remove" -> removeEntityModel(it)
                    else -> error("龙核module书写错误")
                }
            }
            case("view") { setView(it) }
            case("title") { setWindowTitle(it) }
            case("bindEntity", "bind") { bindEntityLocation(it) }
            case("modelEffect") {
                when (it.expects("create", "remove")) {
                    "create" -> createModelEffect(it)
                    "remove" -> removeModelEffect(it)
                    else -> error("龙核modelEffect书写错误")
                }
            }
            case("invisibleHand") { invisibleHand(it) }
            case("slot") { slotItemStack(it) }
        }
    }

    private fun sendArmourers(reader: QuestReader): ScriptAction<Any?> {
        val armourers = reader.nextParsedAction()
        val timeout = reader.nextHeadAction("timeout", def = "100")
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

    private fun updateArmourers(reader: QuestReader): ScriptAction<Any?> {
        val container = reader.nextTheyContainerOrNull()

        return actionNow {
            containerOrSelf(container) { container ->
                container.forEachInstance<PlayerTarget> { player ->
                    DragonAPI.updatePlayerSkin(player.getSource())
                }
            }
        }
    }

    private fun sendEffect(reader: QuestReader): ScriptAction<Any?> {
        val id = reader.nextParsedAction()
        val name = reader.nextParsedAction()
        val rotation = reader.nextHeadAction("rotation", def = "0,0,0")
        val translate = reader.nextHeadAction("translate", def = "")
        val lifeTime = reader.nextHeadAction("timeout", def = 100)
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(id).str { id ->
                run(name).str { name ->
                    run(rotation).str { rotation ->
                        run(translate).vector { translate ->
                            run(lifeTime).int { lifeTime ->
                                container(viewers, serverPlayerContainer) { container0 ->
                                    containerOrSelf(they) { container1 ->
                                        val target = container1.firstInstance<ITargetLocation<*>>()
                                        val posOrEntityUUID = if (target is ITargetEntity<*>) {
                                            target.entity.uniqueId.toString()
                                        } else {
                                            "${target.location.x},${target.location.y},${target.location.z}"
                                        }
                                        container0.forEachInstance<PlayerTarget> { player ->
                                            PacketSender.addParticle(
                                                player.getSource(),
                                                name,
                                                id,
                                                posOrEntityUUID,
                                                rotation,
                                                translate.dragonString(),
                                                lifeTime
                                            )
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
        return when (reader.expects("player", "entity", "item", "block")) {
            "player" -> setPlayerAnimation(reader)
            "entity" -> setModelEntityAnimation(reader)
            "item" -> setItemAnimation(reader)
            "block" -> setBlockAnimation(reader)
            else -> error("龙核set animation类型错误")
        }
    }

    private fun removeAnimation(reader: QuestReader): ScriptAction<Any?> {
        return when (reader.expects("player", "entity")) {
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
                        containerOrSelf(entities) { entities ->
                            container(viewers, serverPlayerContainer) { viewers ->
                                val players = viewers.get<PlayerTarget>()
                                entities.forEachInstance<ITargetEntity<*>> { target ->
                                    if (target is PlayerTarget) return@forEachInstance
                                    players.forEach { viewer ->
                                        PacketSender.setModelEntityAnimation(
                                            viewer.getSource(),
                                            target.entity.uniqueId,
                                            animation,
                                            transitionTime * 50,
                                            speed
                                        )
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
                                    PacketSender.removeModelEntityAnimation(
                                        viewer.getSource(),
                                        target.entity.uniqueId,
                                        animation,
                                        transitionTime * 50
                                    )
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
                    containerOrSelf(entities) { entities ->
                        container(viewers, serverPlayerContainer) { viewers ->
                            val players = viewers.get<PlayerTarget>()
                            entities.forEachInstance<ITargetEntity<*>> { entity ->
                                players.forEach { player ->
                                    DragonCoreCustomPacketSender.setEntityModelItemAnimation(
                                        player.getSource(),
                                        entity.entity.uniqueId,
                                        animation,
                                        speed
                                    )
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
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(xyz).vector { xyz ->
                    containerOrSelf(viewers) { players ->
                        players.forEachInstance<PlayerTarget> { player ->
                            PacketSender.setBlockAnimation(
                                player.getSource(),
                                xyz.x().toInt(),
                                xyz.y().toInt(),
                                xyz.z().toInt(),
                                animation
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sendSound(reader: QuestReader): ScriptAction<Any?> {
        val soundKey = reader.nextParsedAction()
        val soundFile = reader.nextParsedAction()
        val category = reader.nextParsedAction()
        val vector = reader.nextHeadActionOrNull(arrayOf("loc"))
        val loop = reader.nextHeadAction("loop", def = false)
        val (volume, pitch) = try {
            reader.mark()
            reader.expects("by", "with")
            reader.nextParsedAction() to reader.nextParsedAction()
        } catch (e: Exception) {
            reader.reset()
            null to null
        }
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(soundKey).str { soundKey ->
                run(soundFile).str { soundFile ->
                    run(category).str { category ->
                        run(loop).bool { loop ->
                            run(volume ?: literalAction(1.0)).float { volume ->
                                run(pitch ?: literalAction(1.0)).float { pitch ->
                                    containerOrSelf(players) {
                                        if (vector == null) {
                                            it.forEachInstance<PlayerTarget> { player ->
                                                PacketSender.sendPlaySound(
                                                    player.getSource(),
                                                    soundKey,
                                                    soundFile,
                                                    category.lowercase(),
                                                    volume,
                                                    pitch,
                                                    loop,
                                                    player.location.x.cfloat,
                                                    player.location.y.cfloat,
                                                    player.location.z.cfloat
                                                )
                                            }
                                        } else {
                                            run(vector).vector { vector ->
                                                it.forEachInstance<PlayerTarget> { player ->
                                                    PacketSender.sendPlaySound(
                                                        player.getSource(),
                                                        soundKey,
                                                        soundFile,
                                                        category.lowercase(),
                                                        volume,
                                                        pitch,
                                                        loop,
                                                        vector.x().cfloat,
                                                        vector.y().cfloat,
                                                        vector.z().cfloat
                                                    )
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
        }
    }

    private fun stopSound(reader: QuestReader): ScriptAction<Any?> {
        val soundKey = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(soundKey).str { soundKey ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.sendStopSound(player.getSource(), soundKey)
                    }
                }
            }
        }
    }

    private fun openGui(reader: QuestReader): ScriptAction<Any?> {
        val guiName = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(guiName).str { guiName ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.sendOpenGui(player.getSource(), guiName)
                    }
                }
            }
        }
    }

    private fun openHud(reader: QuestReader): ScriptAction<Any?> {
        val guiName = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(guiName).str { guiName ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.sendOpenHud(player.getSource(), guiName)
                    }
                }
            }
        }
    }

    private fun sendSyncPlaceholder(reader: QuestReader): ScriptAction<Any?> {
        val keys = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(keys).str { keys ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.sendSyncPlaceholder(
                            player.getSource(),
                            keys.split(",").associateWith { key -> script().get<Any?>(key).toString() }
                        )
                    }
                }
            }
        }
    }

    private fun deletePlaceholderCache(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val isStartWith = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(key).str { key ->
                run(isStartWith).bool { isStartWith ->
                    containerOrSelf(players) {
                        it.forEachInstance<PlayerTarget> { player ->
                            PacketSender.sendDeletePlaceholderCache(player.getSource(), key, isStartWith)
                        }
                    }
                }
            }
        }
    }

    private fun sendFunction(reader: QuestReader): ScriptAction<Any?> {
        val guiName = reader.nextParsedAction()
        val function = reader.nextParsedAction()
        val async = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(guiName).str { guiName ->
                run(function).str { function ->
                    run(async).bool { async ->
                        containerOrSelf(players) {
                            it.forEachInstance<PlayerTarget> { player ->
                                PacketSender.sendRunFunction(player.getSource(), guiName, function, async)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendAnimationFunction(reader: QuestReader): ScriptAction<Any?> {
        val uuid = reader.nextParsedAction()
        val function = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(uuid).str { uuid ->
                run(function).str { function ->
                    containerOrSelf(players) {
                        val id = uuid.parseUUID()!!
                        if (Bukkit.getPlayer(id) != null) {
                            it.forEachInstance<PlayerTarget> { player ->
                                DragonCoreCustomPacketSender.runPlayerAnimationControllerFunction(
                                    player.getSource(),
                                    id,
                                    function
                                )
                            }
                        } else {
                            it.forEachInstance<PlayerTarget> { player ->
                                PacketSender.runEntityAnimationFunction(player.getSource(), id, function)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendTagFunction(reader: QuestReader): ScriptAction<Any?> {
        val uuid = reader.nextParsedAction()
        val function = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(uuid).str { uuid ->
                run(function).str { function ->
                    containerOrSelf(players) {
                        it.forEachInstance<PlayerTarget> { player ->
                            PacketSender.runEntityTagFunction(player.getSource(), uuid.parseUUID(), function)
                        }
                    }
                }
            }
        }
    }

    private fun setHeadTag(reader: QuestReader): ScriptAction<Any?> {
        val uuid = reader.nextParsedAction()
        val name = reader.nextParsedAction()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(uuid).str { uuid ->
                run(name).str { name ->
                    containerOrSelf(viewers) {
                        it.forEachInstance<PlayerTarget> { player ->
                            PacketSender.setEntityHeadTag(player.getSource(), uuid.parseUUID(), name)
                        }
                    }
                }
            }
        }
    }

    private fun removeHeadTag(reader: QuestReader): ScriptAction<Any?> {
        val uuid = reader.nextParsedAction()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(uuid).str { uuid ->
                containerOrSelf(viewers) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.setEntityHeadTag(player.getSource(), uuid.parseUUID(), null)
                    }
                }
            }
        }
    }

    private fun setEntityModel(reader: QuestReader): ScriptAction<Any?> {
        val uuid = reader.nextParsedAction()
        val name = reader.nextParsedAction()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(uuid).str { uuid ->
                run(name).str { name ->
                    containerOrSelf(viewers) {
                        it.forEachInstance<PlayerTarget> { player ->
                            PacketSender.setEntityModel(player.getSource(), uuid.parseUUID(), name)
                        }
                    }
                }
            }
        }
    }

    private fun removeEntityModel(reader: QuestReader): ScriptAction<Any?> {
        val uuid = reader.nextParsedAction()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(uuid).str { uuid ->
                containerOrSelf(viewers) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.setEntityModel(player.getSource(), uuid.parseUUID(), null)
                    }
                }
            }
        }
    }

    private fun setView(reader: QuestReader): ScriptAction<Any?> {
        val view = reader.nextParsedAction()
        val viewers = reader.nextTheyContainerOrNull()

        return actionNow {
            run(view).int { view ->
                containerOrSelf(viewers) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.setThirdPersonView(player.getSource(), view)
                    }
                }
            }
        }
    }

    private fun setWindowTitle(reader: QuestReader): ScriptAction<Any?> {
        val title = reader.nextParsedAction()
        val viewers = reader.nextTheyContainerOrNull()

        return actionNow {
            run(title).str { title ->
                containerOrSelf(viewers) {
                    it.forEachInstance<PlayerTarget> { player ->
                        PacketSender.setWindowTitle(player.getSource(), title)
                    }
                }
            }
        }
    }

    private fun bindEntityLocation(reader: QuestReader): ScriptAction<Any?> {
        val uuid = reader.nextParsedAction()
        val owner = reader.nextParsedAction()
        val vector = reader.nextParsedAction()
        val bindYaw = reader.nextParsedAction()
        val bindPitch = reader.nextParsedAction()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(uuid).str { uuid ->
                run(owner).str { owner ->
                    run(vector).vector { vector ->
                        run(bindYaw).bool { yaw ->
                            run(bindPitch).bool { pitch ->
                                containerOrSelf(viewers) {
                                    it.forEachInstance<PlayerTarget> { player ->
                                        PacketSender.sendEntityLocationBind(
                                            player.getSource(),
                                            uuid.parseUUID(),
                                            owner.parseUUID(),
                                            vector.x().cfloat,
                                            vector.y().cfloat,
                                            vector.z().cfloat,
                                            yaw,
                                            pitch
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createModelEffect(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val model = reader.nextParsedAction()
        val timeout = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionFuture { future ->
            run(key).str { key ->
                run(model).str { model ->
                    run(timeout).long { timeout ->
                        containerOrSelf(they) { container ->
                            ensureSync {
                                container.mapInstance<ITargetEntity<*>, ModelEffect> { target ->
                                    sendModelEffect(target.entity, key, model, timeout)
                                }
                            }.thenAccept {
                                addOrryxCloseable(CompletableFuture.allOf(*it.map { modelEffect -> modelEffect.simpleTimeoutTask.future }
                                    .toTypedArray())) {
                                    it.forEach { modelEffect0 ->
                                        effectMap[modelEffect0.owner]?.removeIf { modelEffect1 ->
                                            modelEffect0 == modelEffect1
                                        }
                                        modelEffect0.entity.entity.remove()
                                    }
                                }
                                future.complete(Container(it.mapTo(linkedSetOf()) { modelEffect -> modelEffect.entity }))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeModelEffect(reader: QuestReader): ScriptAction<Any?> {
        val key = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(key).str { key ->
                containerOrSelf(they) { container ->
                    container.forEachInstance<ITargetEntity<*>> { target ->
                        val iterator = effectMap[target.entity.uniqueId]?.iterator() ?: return@forEachInstance
                        while (iterator.hasNext()) {
                            val modelEffect = iterator.next()
                            if (modelEffect.entity.getSource().id == key) {
                                SimpleTimeoutTask.cancel(modelEffect.simpleTimeoutTask)
                            }
                        }
                    }
                }
            }
        }
    }

    class ModelEffect(val owner: UUID, val entity: ITargetEntity<EntityInstance>) {
        lateinit var simpleTimeoutTask: SimpleTimeoutTask
    }

    private fun sendModelEffect(entity: IEntity, key: String, model: String, timeout: Long): ModelEffect {
        val instance = Adyeshach.api().getPublicEntityManager().create(EntityTypes.ARMOR_STAND, entity.location) {
            it.id = key
        }
        val effect: ITargetEntity<EntityInstance> = AbstractAdyeshachEntity(instance)
//        when(entity) {
//            is AbstractBukkitEntity, is PlayerTarget -> {
//                val craftWorld = entity.world as CraftWorld
//                val effectEntity = craftWorld.addEntity<CraftArmorStand>(
//                    craftWorld.createEntity(entity.location, ArmorStand::class.java),
//                    CreatureSpawnEvent.SpawnReason.CUSTOM
//                ) {
//                    it.customName = key
//                    it.setMeta(IGNORE_HIT, true)
//                    it.setGravity(false)
//                    it.isSilent = true
//                    it.isInvulnerable = true
//                    it.isMarker = true
//                }
//                AbstractBukkitEntity(effectEntity)
//            }
//
//            is AbstractAdyeshachEntity -> {
//            }
//
//            else -> error("使用了不支持modelEffect的实体类型")
//        }
        onlinePlayers.forEach { player ->
            PacketSender.setEntityModel(player, effect.entity.uniqueId, model)
            PacketSender.sendEntityLocationBind(
                player,
                effect.entity.uniqueId,
                entity.uniqueId,
                0.0f,
                0.0f,
                0.0f,
                true,
                true
            )
        }
        val modelEffect = ModelEffect(entity.uniqueId, effect)
        effectMap.getOrPut(entity.uniqueId) { mutableListOf() }.add(modelEffect)
        modelEffect.simpleTimeoutTask = SimpleTimeoutTask.createSimpleTask(timeout) {
            effectMap[entity.uniqueId]?.remove(modelEffect)
            if (effectMap[entity.uniqueId]?.isEmpty() == true) {
                effectMap.remove(entity.uniqueId)
            }
            effect.entity.remove()
        }
        return modelEffect
    }

    private fun invisibleHand(reader: QuestReader): ScriptAction<Any?> {
        val tick = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()
        return actionNow {
            run(tick).long { tick ->
                if (tick > 0) {
                    containerOrSelf(players) { container ->
                        container.forEachInstance<PlayerTarget> { target ->
                            StateManager.setInvisibleHand(target.getSource(), tick)
                        }
                    }
                } else {
                    containerOrSelf(players) { container ->
                        container.forEachInstance<PlayerTarget> { target ->
                            StateManager.cancelInvisibleHand(target.getSource())
                        }
                    }
                }
            }
        }
    }

    private fun slotItemStack(reader: QuestReader): ScriptAction<Any?> {
        val identifier = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()
        return actionFuture { future ->
            run(identifier).str { identifier ->
                containerOrSelf(players) { container ->
                    val target = container.firstInstance<PlayerTarget>()
                    SlotAPI.getSlotItem(target.getSource(), identifier, object : IDataBase.Callback<ItemStack> {

                        override fun onResult(item: ItemStack?) {
                            future.complete(item)
                        }
                        override fun onFail() {
                            future.completeExceptionally(Throwable("Failed to get slot item $identifier"))
                        }
                    })
                }
            }
        }
    }
}