package org.gitee.orryx.core.kether.actions.compat.arcartx

import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.api.ArcartXAPI
import priv.seventeen.artist.arcartx.internal.network.NetworkMessageSender
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object ArcartXActions {

    @KetherParser(["arcartx", "ax"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun arcartX() = scriptParser(
        Action.new("ArcartX附属语句", "设置实体动画", "arcartx", true)
            .description("设置实体动画")
            .addEntry("动画标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
            .addEntry("动画名", Type.STRING, false)
            .addEntry("动画速度", Type.FLOAT, true, "1.0", "speed")
            .addEntry("过渡时间tick", Type.INT, true, "0", "transition")
            .addEntry("持续时间tick", Type.LONG, true, "-1", "duration")
            .addContainerEntry("设置实体", false)
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("ArcartX附属语句", "设置实体默认动画状态", "arcartx", true)
            .description("设置实体默认动画状态")
            .addEntry("动画标识符", Type.SYMBOL, false, head = "animation/ani")
            .addEntry("默认标识符", Type.SYMBOL, false, head = "default")
            .addEntry("动画名", Type.STRING, false)
            .addEntry("状态名", Type.STRING, false)
            .addContainerEntry("设置实体", false)
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("ArcartX附属语句", "播放音效", "arcartx", true)
            .description("播放音效")
            .addEntry("音效标识符", Type.SYMBOL, false, head = "sound")
            .addEntry("发送标识符", Type.SYMBOL, false, head = "send")
            .addEntry("音效文件", Type.STRING, false)
            .addEntry("音效类型", Type.STRING, true, "master", "category")
            .addEntry("音量", Type.FLOAT, true, "1.0", "volume")
            .addEntry("音调", Type.FLOAT, true, "1.0", "pitch")
            .addContainerEntry("可听玩家", true, "@self"),
        Action.new("ArcartX附属语句", "停止音效", "arcartx", true)
            .description("停止音效")
            .addEntry("音效标识符", Type.SYMBOL, false, head = "sound")
            .addEntry("停止标识符", Type.SYMBOL, false, head = "stop")
            .addEntry("音效名", Type.STRING, false)
            .addContainerEntry("可听玩家", true, "@self"),
        Action.new("ArcartX附属语句", "打开UI", "arcartx", true)
            .description("打开ArcartX UI")
            .addEntry("ui标识符", Type.SYMBOL, false, head = "ui")
            .addEntry("打开标识符", Type.SYMBOL, false, head = "open")
            .addEntry("ui名字", Type.STRING, false)
            .addContainerEntry("打开UI的玩家", true, "@self"),
        Action.new("ArcartX附属语句", "关闭UI", "arcartx", true)
            .description("关闭ArcartX UI")
            .addEntry("ui标识符", Type.SYMBOL, false, head = "ui")
            .addEntry("关闭标识符", Type.SYMBOL, false, head = "close")
            .addEntry("ui名字", Type.STRING, false)
            .addContainerEntry("关闭UI的玩家", true, "@self"),
        Action.new("ArcartX附属语句", "运行UI脚本", "arcartx", true)
            .description("运行ArcartX UI脚本")
            .addEntry("ui标识符", Type.SYMBOL, false, head = "ui")
            .addEntry("运行标识符", Type.SYMBOL, false, head = "run")
            .addEntry("ui名字", Type.STRING, false)
            .addEntry("脚本内容", Type.STRING, false)
            .addContainerEntry("执行的玩家", true, "@self"),
        Action.new("ArcartX附属语句", "设置实体模型", "arcartx", true)
            .description("设置实体模型")
            .addEntry("模型标识符", Type.SYMBOL, false, head = "model")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
            .addEntry("模型名", Type.STRING, false)
            .addEntry("模型缩放", Type.FLOAT, true, "1.0", "scale")
            .addContainerEntry("设置实体", false)
            .addContainerEntry("可视玩家", true, "@server", "viewers"),
        Action.new("ArcartX附属语句", "设置服务端变量", "arcartx", true)
            .description("设置服务端变量")
            .addEntry("变量标识符", Type.SYMBOL, false, head = "variable/var")
            .addEntry("设置标识符", Type.SYMBOL, false, head = "set/to")
            .addEntry("变量名", Type.STRING, false)
            .addEntry("变量值", Type.STRING, false)
            .addContainerEntry("设置的玩家", true, "@self"),
        Action.new("ArcartX附属语句", "移除服务端变量", "arcartx", true)
            .description("移除服务端变量")
            .addEntry("变量标识符", Type.SYMBOL, false, head = "variable/var")
            .addEntry("移除标识符", Type.SYMBOL, false, head = "remove")
            .addEntry("变量名", Type.STRING, false)
            .addContainerEntry("移除的玩家", true, "@self"),
        Action.new("ArcartX附属语句", "发送自定义数据包", "arcartx", true)
            .description("发送自定义数据包")
            .addEntry("数据包标识符", Type.SYMBOL, false, head = "packet")
            .addEntry("数据包ID", Type.STRING, false)
            .addEntry("数据内容(逗号分隔)", Type.STRING, false)
            .addContainerEntry("发送的玩家", true, "@self"),
        Action.new("ArcartX附属语句", "屏幕震动", "arcartx", true)
            .description("屏幕震动")
            .addEntry("震动标识符", Type.SYMBOL, false, head = "shake")
            .addEntry("震动强度", Type.INT, false)
            .addEntry("震动时长tick", Type.INT, false)
            .addContainerEntry("震动的玩家", true, "@self"),
        Action.new("ArcartX附属语句", "设置窗口标题", "arcartx", true)
            .description("设置窗口标题")
            .addEntry("标题标识符", Type.SYMBOL, false, head = "title")
            .addEntry("标题内容", Type.STRING, false)
            .addContainerEntry("设置的玩家", true, "@self")
    ) {
        it.switch {
            case("animation", "ani") {
                when (it.expects("set", "to", "default")) {
                    "set", "to" -> setEntityAnimation(it)
                    "default" -> setEntityDefaultState(it)
                    else -> error("AX animation书写错误")
                }
            }
            case("sound") {
                when (it.expects("send", "stop")) {
                    "send" -> sendSound(it)
                    "stop" -> stopSound(it)
                    else -> error("AX sound书写错误")
                }
            }
            case("ui") {
                when (it.expects("open", "close", "run")) {
                    "open" -> openUI(it)
                    "close" -> closeUI(it)
                    "run" -> runUI(it)
                    else -> error("AX ui书写错误")
                }
            }
            case("model") {
                it.expects("set", "to")
                setEntityModel(it)
            }
            case("variable", "var") {
                when (it.expects("set", "to", "remove")) {
                    "set", "to" -> setVariable(it)
                    "remove" -> removeVariable(it)
                    else -> error("AX variable书写错误")
                }
            }
            case("packet") { sendPacket(it) }
            case("shake") { shake(it) }
            case("title") { setTitle(it) }
        }
    }

    private fun setEntityAnimation(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val speed = reader.nextHeadAction("speed", def = 1.0)
        val transition = reader.nextHeadAction("transition", def = 0)
        val duration = reader.nextHeadAction("duration", def = -1L)
        val entities = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(speed).float { speed ->
                    run(transition).int { transition ->
                        run(duration).long { duration ->
                            entities ?: error("AX动画无设置实体")
                            containerOrSelf(entities) { entities ->
                                container(viewers, serverPlayerContainer) { viewers ->
                                    val players = viewers.get<PlayerTarget>()
                                    entities.forEachInstance<ITargetEntity<*>> { target ->
                                        players.forEach { viewer ->
                                            NetworkMessageSender.sendEntityAnimation(
                                                viewer.getSource(),
                                                target.entity.uniqueId,
                                                animation,
                                                speed.toDouble(),
                                                transition,
                                                duration
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

    private fun setEntityDefaultState(reader: QuestReader): ScriptAction<Any?> {
        val animation = reader.nextParsedAction()
        val state = reader.nextParsedAction()
        val entities = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(animation).str { animation ->
                run(state).str { state ->
                    entities ?: error("AX动画无设置实体")
                    containerOrSelf(entities) { entities ->
                        container(viewers, serverPlayerContainer) { viewers ->
                            val players = viewers.get<PlayerTarget>()
                            entities.forEachInstance<ITargetEntity<*>> { target ->
                                players.forEach { viewer ->
                                    NetworkMessageSender.sendEntityDefaultAnimationState(
                                        viewer.getSource(),
                                        target.entity.uniqueId,
                                        animation,
                                        state
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendSound(reader: QuestReader): ScriptAction<Any?> {
        val soundFile = reader.nextParsedAction()
        val category = reader.nextHeadAction("category", def = "master")
        val volume = reader.nextHeadAction("volume", def = 1.0)
        val pitch = reader.nextHeadAction("pitch", def = 1.0)
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(soundFile).str { soundFile ->
                run(category).str { category ->
                    run(volume).float { volume ->
                        run(pitch).float { pitch ->
                            containerOrSelf(players) {
                                it.forEachInstance<PlayerTarget> { player ->
                                    val loc = player.location
                                    NetworkMessageSender.sendPlaySound(
                                        player.getSource(),
                                        soundFile,
                                        loc.x.toInt(), loc.y.toInt(), loc.z.toInt(),
                                        category,
                                        0,
                                        volume.toDouble(),
                                        pitch.toInt()
                                    )
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
                        NetworkMessageSender.sendStopSound(player.getSource(), soundName)
                    }
                }
            }
        }
    }

    private fun openUI(reader: QuestReader): ScriptAction<Any?> {
        val uiName = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(uiName).str { uiName ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        ArcartXAPI.getUIRegistry().open(player.getSource(), uiName)
                    }
                }
            }
        }
    }

    private fun closeUI(reader: QuestReader): ScriptAction<Any?> {
        val uiName = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(uiName).str { uiName ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        ArcartXAPI.getUIRegistry().close(player.getSource(), uiName)
                    }
                }
            }
        }
    }

    private fun runUI(reader: QuestReader): ScriptAction<Any?> {
        val uiName = reader.nextParsedAction()
        val script = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(uiName).str { uiName ->
                run(script).str { script ->
                    containerOrSelf(players) {
                        it.forEachInstance<PlayerTarget> { player ->
                            ArcartXAPI.getUIRegistry().run(player.getSource(), uiName, script)
                        }
                    }
                }
            }
        }
    }

    private fun setEntityModel(reader: QuestReader): ScriptAction<Any?> {
        val modelName = reader.nextParsedAction()
        val scale = reader.nextHeadAction("scale", def = 1.0)
        val entities = reader.nextTheyContainerOrNull()
        val viewers = reader.nextHeadActionOrNull(arrayOf("viewers"))

        return actionNow {
            run(modelName).str { modelName ->
                run(scale).float { scale ->
                    entities ?: error("AX模型无设置实体")
                    containerOrSelf(entities) { entities ->
                        container(viewers, serverPlayerContainer) { viewers ->
                            val players = viewers.get<PlayerTarget>()
                            entities.forEachInstance<ITargetEntity<*>> { target ->
                                players.forEach { viewer ->
                                    NetworkMessageSender.setEntityModel(
                                        viewer.getSource(),
                                        target.entity.uniqueId,
                                        modelName,
                                        scale.toDouble()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setVariable(reader: QuestReader): ScriptAction<Any?> {
        val name = reader.nextParsedAction()
        val value = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(name).str { name ->
                run(value).str { value ->
                    containerOrSelf(players) {
                        it.forEachInstance<PlayerTarget> { player ->
                            NetworkMessageSender.sendServerVariable(player.getSource(), name, value)
                        }
                    }
                }
            }
        }
    }

    private fun removeVariable(reader: QuestReader): ScriptAction<Any?> {
        val name = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(name).str { name ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        NetworkMessageSender.sendRemoveServerVariable(player.getSource(), name, false)
                    }
                }
            }
        }
    }

    private fun sendPacket(reader: QuestReader): ScriptAction<Any?> {
        val id = reader.nextParsedAction()
        val data = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(id).str { id ->
                run(data).str { data ->
                    containerOrSelf(players) {
                        it.forEachInstance<PlayerTarget> { player ->
                            NetworkMessageSender.sendCustomPacket(player.getSource(), id, *data.split(",").toTypedArray())
                        }
                    }
                }
            }
        }
    }

    private fun shake(reader: QuestReader): ScriptAction<Any?> {
        val intensity = reader.nextParsedAction()
        val duration = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(intensity).int { intensity ->
                run(duration).int { duration ->
                    containerOrSelf(players) {
                        it.forEachInstance<PlayerTarget> { player ->
                            NetworkMessageSender.sendShake(player.getSource(), intensity, duration)
                        }
                    }
                }
            }
        }
    }

    private fun setTitle(reader: QuestReader): ScriptAction<Any?> {
        val title = reader.nextParsedAction()
        val players = reader.nextTheyContainerOrNull()

        return actionNow {
            run(title).str { title ->
                containerOrSelf(players) {
                    it.forEachInstance<PlayerTarget> { player ->
                        NetworkMessageSender.sendClientTitle(player.getSource(), title)
                    }
                }
            }
        }
    }
}
