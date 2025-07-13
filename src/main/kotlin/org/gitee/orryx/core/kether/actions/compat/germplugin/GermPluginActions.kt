package org.gitee.orryx.core.kether.actions.compat.germplugin

import com.germ.germplugin.api.*
import com.germ.germplugin.api.bean.AnimDataDTO
import com.germ.germplugin.api.dynamic.skin.GermSkinBedrock
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common5.cdouble
import taboolib.common5.cfloat
import taboolib.common5.cint
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.*

object GermPluginActions {

    private val armourersMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { hashMapOf<UUID, MutableList<String>>() }

    @KetherParser(["germplugin", "germ"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun germplugin() = scriptParser(
        Action.new("germplugin附属语句", "设置临时时装", "germplugin", true)
            .description("设置临时基岩时装")
            .addEntry("armourers标识符", Type.SYMBOL, false, head = "armourers")
            .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
            .addEntry("时装名", Type.STRING, false)
            .addEntry("临时时长", Type.LONG, true, "100", "timeout")
            .addContainerEntry(optional = true, default = "@self"),
        Action.new("germplugin附属语句", "清除临时时装", "germplugin", true)
            .description("清除临时基岩时装")
            .addEntry("armourers标识符", Type.SYMBOL, false, head = "armourers")
            .addEntry("清除标识符", Type.SYMBOL, false, head = "clear")
            .addEntry("时装名", Type.STRING, true, "ALL")
            .addContainerEntry(optional = true, default = "@self"),
        Action.new("germplugin附属语句", "发送effect特效", "germplugin", true)
            .description("发送effect特效")
            .addEntry("特效标识符", Type.SYMBOL, false, head = "effect")
            .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
            .addEntry("Index", Type.STRING, false)
            .addEntry("特效名", Type.STRING, false)
            .addEntry("x,y,z旋转角度", Type.STRING, false, "0,0,0", head = "rotation")
            .addEntry("x,y,z平移位置", Type.VECTOR, false, "0,0,0", head = "translate")
            .addEntry("存活时长tick", Type.INT, false, "100", head = "timeout")
            .addContainerEntry("可视玩家", true, "@server", "viewers")
            .addContainerEntry("生成位置或者绑定实体", true, "@self"),
        Action.new("germplugin附属语句", "移除effect特效", "germplugin", true)
            .description("移除effect特效")
            .addEntry("特效标识符", Type.SYMBOL, false, head = "effect")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("Index", Type.STRING, false)
            .addContainerEntry("可视玩家", true, "@server"),
        Action.new("germplugin附属语句", "清除effect特效", "germplugin", true)
            .description("清除effect特效")
            .addEntry("特效标识符", Type.SYMBOL, false, head = "effect")
            .addEntry("清除标识符", Type.SYMBOL, false, head = "clear")
            .addContainerEntry("可视玩家", true, "@server"),
        // 实体
        Action.new("germplugin附属语句", "设置实体动作", "germplugin", true)
            .description("设置实体动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
            .addEntry("实体标识符", Type.SYMBOL, false, head = "entity")
            .addEntry("动作名", Type.STRING, false)
            .addEntry("播放速度", Type.FLOAT, false)
            .addEntry("是否倒放", Type.BOOLEAN, false)
            .addContainerEntry("设置实体(自动识别玩家还是怪物)", false)
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("germplugin附属语句", "停止实体动作", "germplugin", true)
            .description("停止实体动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("停止标识符", Type.SYMBOL, false, head = "stop")
            .addEntry("实体标识符", Type.SYMBOL, false, head = "entity")
            .addEntry("动作名", Type.STRING, false)
            .addContainerEntry("设置实体(自动识别玩家还是怪物)", true, "@self")
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("germplugin附属语句", "移除实体动作", "germplugin", true)
            .description("移除实体动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("实体标识符", Type.SYMBOL, false, head = "entity")
            .addContainerEntry("设置实体(自动识别玩家还是怪物)", false)
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        // 物品
        Action.new("germplugin附属语句", "设置玩家物品动作", "germplugin", true)
            .description("设置玩家物品动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
            .addEntry("物品标识符", Type.SYMBOL, false, head = "item")
            .addEntry("槽位名", Type.STRING, false)
            .addEntry("动作名", Type.STRING, false)
            .addEntry("播放速度", Type.FLOAT, false)
            .addEntry("是否倒放", Type.BOOLEAN, false)
            .addContainerEntry("设置实体", true, "@self")
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("germplugin附属语句", "停止玩家物品动作", "germplugin", true)
            .description("停止玩家物品动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("停止标识符", Type.SYMBOL, false, head = "stop")
            .addEntry("物品标识符", Type.SYMBOL, false, head = "item")
            .addEntry("槽位名", Type.STRING, false)
            .addEntry("动作名", Type.STRING, false)
            .addContainerEntry("设置实体", true, "@self")
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("germplugin附属语句", "移除玩家物品动作", "germplugin", true)
            .description("移除玩家物品动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("物品标识符", Type.SYMBOL, false, head = "item")
            .addEntry("槽位名", Type.STRING, false)
            .addContainerEntry("设置实体", true, "@self")
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        // 方块
        Action.new("germplugin附属语句", "设置方块动作", "germplugin", true)
            .description("设置方块动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set")
            .addEntry("方块标识符", Type.SYMBOL, false, head = "block")
            .addEntry("动作名", Type.STRING, false)
            .addEntry("播放速度", Type.FLOAT, false)
            .addEntry("是否倒放", Type.BOOLEAN, false)
            .addEntry("xyz位置", Type.VECTOR, false)
            .addContainerEntry("可视玩家", true, "@self", "viewers"),
        Action.new("germplugin附属语句", "停止方块动作", "germplugin", true)
            .description("停止方块动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("停止标识符", Type.SYMBOL, false, head = "stop")
            .addEntry("方块标识符", Type.SYMBOL, false, head = "block")
            .addEntry("动作名", Type.STRING, false)
            .addEntry("xyz位置", Type.VECTOR, false)
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("germplugin附属语句", "移除方块动作", "germplugin", true)
            .description("移除方块动作")
            .addEntry("动作标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("方块标识符", Type.SYMBOL, false, head = "block")
            .addEntry("xyz位置", Type.VECTOR, false)
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("germplugin附属语句", "播放音乐", "germplugin", true)
            .description("播放音乐")
            .addEntry("音乐标识符", Type.SYMBOL, false, head = "sound")
            .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
            .addEntry("音乐名", Type.STRING, false)
            .addEntry("播放类型", Type.STRING, false)
            .addEntry("播放世界位置向量", Type.VECTOR, true, "可听玩家眼睛位置", "loc")
            .addEntry("是否循环", Type.BOOLEAN, true, head = "loop", default = "false")
            .addEntry("声音大小", Type.FLOAT, true, head = "by/with", default = "1.0")
            .addEntry("声音音调", Type.FLOAT, true, default = "1.0")
            .addContainerEntry("可听玩家", true, "@self"),
        Action.new("germplugin附属语句", "停止播放音乐", "germplugin", true)
            .description("停止播放音乐")
            .addEntry("音乐名", Type.STRING, false)
            .addContainerEntry("可听玩家", true, "@self"),
        Action.new("germplugin附属语句", "打开GUI", "germplugin", true)
            .description("打开萌芽Gui")
            .addEntry("gui标识符", Type.SYMBOL, false, head = "gui")
            .addEntry("gui名字", Type.STRING, false)
            .addContainerEntry("打开GUI的玩家", true, "@self"),
        Action.new("germplugin附属语句", "打开HUD", "germplugin", true)
            .description("打开萌芽HUD")
            .addEntry("hud标识符", Type.SYMBOL, false, head = "hud")
            .addEntry("hud名字", Type.STRING, false)
            .addContainerEntry("打开HUD的玩家", true, "@self"),
        Action.new("germplugin附属语句", "设置视角", "germplugin", true)
            .description("设置第几人称视角")
            .addEntry("视角标识符", Type.SYMBOL, false, head = "view")
            .addEntry("视角(FIRST_PERSON, THIRD_PERSON_REVERSE, CURRENT_PERSON, THIRD_PERSON)", Type.STRING, false)
            .addContainerEntry("设置人称的玩家", true, "@self"),
        Action.new("germplugin附属语句", "获取槽位内物品", "germplugin", true)
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
                    else -> error("萌芽armourers书写错误")
                }
            }
            case("effect") {
                when (it.expects("send", "remove", "clear")) {
                    "send" -> sendEffect(it)
                    "remove" -> removeEffect(it)
                    "clear" -> clearEffect(it)
                    else -> error("萌芽effect书写错误")
                }
            }
            case("animation", "ani") {
                when (it.expects("set", "to", "stop", "remove")) {
                    "set", "to" -> setAnimation(it)
                    "stop" -> stopAnimation(it)
                    "remove" -> removeAnimation(it)
                    else -> error("萌芽animation书写错误")
                }
            }
            case("sound") {
                when (it.expects("send", "stop")) {
                    "send" -> sendSound(it)
                    "stop" -> stopSound(it)
                    else -> error("萌芽sound书写错误")
                }
            }
            case("gui") { openGui(it) }
            case("hud") { openHud(it) }
            case("view") { setView(it) }
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
                            val skin = GermSkinBedrock().setIdentity(armourers)
                            GermSkinAPI.addBedrockSkin(player.getSource(), skin)
                            SimpleTimeoutTask.createSimpleTask(timeout) {
                                if (armourersMap.containsKey(player.uniqueId)) {
                                    armourersMap[player.uniqueId]!!.remove(armourers)
                                    GermSkinAPI.removeBedrockSkin(player.getSource(), armourers)
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
                                GermSkinAPI.removeBedrockSkin(player.getSource(), armourers)
                            }
                        }
                    }
                }
            } else {
                containerOrSelf(container1) { container ->
                    container.forEachInstance<PlayerTarget> { player ->
                        if (armourersMap.containsKey(player.uniqueId)) {
                            armourersMap.remove(player.uniqueId)?.forEach {
                                GermSkinAPI.removeBedrockSkin(player.getSource(), it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendEffect(reader: QuestReader): ScriptAction<Any?> {
        val id = reader.nextParsedAction()
        val name = reader.nextParsedAction()
        val rotation = reader.nextHeadAction("rotation", def = "0,0,0")
        val translate = reader.nextHeadAction("translate", def = "")
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(id).str { id ->
                run(name).str { name ->
                    run(rotation).str { rotation ->
                        run(translate).vector { translate ->
                            containerOrSelf(they) { location ->
                                val rotation = rotation.split(",").map { it.cdouble }
                                container(viewers, serverPlayerContainer) {
                                    val location = location.firstInstance<ITargetLocation<*>>().location
                                    it.forEachInstance<PlayerTarget> { target ->
                                        GermPacketAPI.sendEffect(
                                            target.getSource(),
                                            name,
                                            id,
                                            location.x + translate.x(),
                                            location.y + translate.y(),
                                            location.z + translate.z(),
                                            location.pitch.cdouble + rotation[2],
                                            location.yaw.cdouble + rotation[1], 
                                            rotation[0]
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

    private fun removeEffect(reader: QuestReader): ScriptAction<Any?> {
        val id = reader.nextParsedAction()
        val they = reader.nextTheyContainerOrNull()

        return actionNow {
            run(id).str { id ->
                container(they, serverPlayerContainer) {
                    it.forEachInstance<PlayerTarget> { target ->
                        GermPacketAPI.removeEffect(target.getSource(), id)
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
                    GermPacketAPI.clearEffect(target.getSource())
                }
            }
        }
    }

    private fun setAnimation(reader: QuestReader): ScriptAction<Any?> {
        return when (reader.expects("entity", "item", "block")) {
            "entity" -> setEntityAnimation(reader)
            "item" -> setItemAnimation(reader)
            "block" -> setBlockAnimation(reader)
            else -> error("萌芽set animation类型错误")
        }
    }

    private fun stopAnimation(reader: QuestReader): ScriptAction<Any?> {
        return when (reader.expects("entity")) {
            "entity" -> stopEntityAnimation(reader)
            "item" -> stopItemAnimation(reader)
            "block" -> stopBlockAnimation(reader)
            else -> error("萌芽stop animation类型错误")
        }
    }

    private fun removeAnimation(reader: QuestReader): ScriptAction<Any?> {
        return when (reader.expects("entity")) {
            "entity" -> removeEntityAnimation(reader)
            "item" -> removeItemAnimation(reader)
            "block" -> removeBlockAnimation(reader)
            else -> error("萌芽remove animation类型错误")
        }
    }

    private fun setEntityAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val speed = reader.nextParsedAction()
        val reverse = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(speed).float { speed ->
                    run(reverse).bool { reverse ->
                        containerOrSelf(players) { players ->
                            container(viewers, serverPlayerContainer) {
                                val animation = AnimDataDTO(animation, speed, reverse)
                                players.forEachInstance<PlayerTarget> { player ->
                                    it.forEachInstance<PlayerTarget> { viewer ->
                                        GermPacketAPI.sendModelAnimation(
                                            viewer.getSource(),
                                            player.entityId,
                                            animation
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

    private fun stopEntityAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                containerOrSelf(players) { players ->
                    container(viewers, serverPlayerContainer) {
                        players.forEachInstance<PlayerTarget> { player ->
                            it.forEachInstance<PlayerTarget> { viewer ->
                                GermPacketAPI.stopModelAnimation(viewer.getSource(), player.entityId, animation)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeEntityAnimation(reader: QuestReader): ScriptAction<Any?> {
        val players = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            containerOrSelf(players) { players ->
                container(viewers, serverPlayerContainer) {
                    players.forEachInstance<PlayerTarget> { player ->
                        it.forEachInstance<PlayerTarget> { viewer ->
                            GermPacketAPI.clearModelAnimation(viewer.getSource(), player.entityId)
                        }
                    }
                }
            }
        }
    }

    private fun setItemAnimation(reader: QuestReader): ScriptAction<Any?> {
        val identity = reader.nextParsedAction()
        val animation = reader.nextParsedAction()
        val speed = reader.nextParsedAction()
        val reverse = reader.nextParsedAction()
        val entities = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(identity).str { identity ->
                run(animation).str { animation ->
                    run(speed).float { speed ->
                        run(reverse).bool { reverse ->
                            containerOrSelf(entities) { entities ->
                                container(viewers, serverPlayerContainer) { viewers ->
                                    val players = viewers.get<PlayerTarget>()
                                    entities.forEachInstance<ITargetEntity<*>> { entity ->
                                        players.forEach { player ->
                                            GermPacketAPI.sendItemAnimation(
                                                player.getSource(),
                                                entity.entity.entityId,
                                                identity,
                                                AnimDataDTO(animation, speed, reverse)
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

    private fun stopItemAnimation(reader: QuestReader): ScriptAction<Any?> {
        val identity = reader.nextParsedAction()
        val animation = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(identity).str { identity ->
                run(animation).str { animation ->
                    containerOrSelf(players) { players ->
                        container(viewers, serverPlayerContainer) {
                            players.forEachInstance<PlayerTarget> { player ->
                                it.forEachInstance<PlayerTarget> { viewer ->
                                    GermPacketAPI.stopItemAnimation(
                                        viewer.getSource(),
                                        player.entityId,
                                        identity,
                                        animation
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeItemAnimation(reader: QuestReader): ScriptAction<Any?> {
        val identity = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(identity).str { identity ->
                containerOrSelf(players) { players ->
                    container(viewers, serverPlayerContainer) {
                        players.forEachInstance<PlayerTarget> { player ->
                            it.forEachInstance<PlayerTarget> { viewer ->
                                GermPacketAPI.clearItemAnimation(viewer.getSource(), player.entityId, identity)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setBlockAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val speed = reader.nextParsedAction()
        val reverse = reader.nextParsedAction()
        val xyz = reader.nextParsedAction()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(speed).float { speed ->
                    run(reverse).bool { reverse ->
                        run(xyz).vector { xyz ->
                            containerOrSelf(viewers) { players ->
                                players.forEachInstance<PlayerTarget> { player ->
                                    GermPacketAPI.sendModelBlockAnimation(
                                        player.getSource(),
                                        xyz.x().cint,
                                        xyz.y().cint,
                                        xyz.z().cint,
                                        AnimDataDTO(animation, speed, reverse)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopBlockAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val xyz = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(xyz).vector { xyz ->
                    containerOrSelf(players) { players ->
                        container(viewers, serverPlayerContainer) {
                            players.forEachInstance<PlayerTarget> { player ->
                                it.forEachInstance<PlayerTarget> { viewer ->
                                    GermPacketAPI.stopModelBlockAnimation(
                                        viewer.getSource(),
                                        xyz.x().cint,
                                        xyz.y().cint,
                                        xyz.z().cint,
                                        animation
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun removeBlockAnimation(reader: QuestReader): ScriptAction<Any?> {
        val xyz = reader.nextParsedAction()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(xyz).vector { xyz ->
                container(viewers, serverPlayerContainer) {
                    it.forEachInstance<PlayerTarget> { viewer ->
                        GermPacketAPI.clearModelBlockAnimation(
                            viewer.getSource(),
                            xyz.x().cint,
                            xyz.y().cint,
                            xyz.z().cint
                        )
                    }
                }
            }
        }
    }

    private fun sendSound(reader: QuestReader): ScriptAction<Any?> {
        val soundName = reader.nextParsedAction()
        val category = reader.nextParsedAction()
        val vector = reader.nextHeadActionOrNull(arrayOf("loc"))
        val loop = reader.nextHeadAction("loop", def = false)
        val (volume, pitch) = try {
            reader.mark()
            reader.expects("by", "with")
            reader.nextParsedAction() to reader.nextParsedAction()
        } catch (_: Exception) {
            reader.reset()
            null to null
        }
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(soundName).str { soundName ->
                run(category).str { category ->
                    run(loop).bool { loop ->
                        run(volume ?: literalAction(1.0)).float { volume ->
                            run(pitch ?: literalAction(1.0)).float { pitch ->
                                containerOrSelf(players) {
                                    val type = SoundType.valueOf(category.uppercase())
                                    if (vector == null) {
                                        it.forEachInstance<PlayerTarget> { player ->
                                            GermPacketAPI.playSound(
                                                player.getSource(),
                                                soundName,
                                                type,
                                                player.location.x.cfloat,
                                                player.location.y.cfloat,
                                                player.location.z.cfloat,
                                                0,
                                                volume,
                                                pitch,
                                                loop,
                                                0
                                            )
                                        }
                                    } else {
                                        run(vector).vector { vector ->
                                            it.forEachInstance<PlayerTarget> { player ->
                                                GermPacketAPI.playSound(
                                                    player.getSource(),
                                                    soundName,
                                                    type,
                                                    vector.x().cfloat,
                                                    vector.y().cfloat,
                                                    vector.z().cfloat,
                                                    0,
                                                    volume,
                                                    pitch,
                                                    loop,
                                                    0
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

    private fun stopSound(reader: QuestReader): ScriptAction<Any?> {
        val soundName = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(soundName).str { soundName ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        GermPacketAPI.stopSound(player.getSource(), soundName)
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
                        GermPacketAPI.openGui(player.getSource(), guiName)
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
                        GermPacketAPI.openHud(player.getSource(), guiName)
                    }
                }
            }
        }
    }

    private fun setView(reader: QuestReader): ScriptAction<Any?> {
        val view = reader.nextParsedAction()
        val viewers = reader.nextTheyContainerOrNull()

        return actionNow {
            run(view).str { view ->
                containerOrSelf(viewers) {
                    val type = ViewType.valueOf(view.uppercase())
                    it.forEachInstance<PlayerTarget> { player ->
                        GermPacketAPI.sendPlayerCameraView(player.getSource(), type)
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
                    future.complete(GermSlotAPI.getGermSlotItemStacks(target.name, listOf(identifier)).firstOrNull())
                }
            }
        }
    }
}