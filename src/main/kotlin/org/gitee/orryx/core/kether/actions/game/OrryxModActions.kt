package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common5.cfloat
import taboolib.module.kether.*

object OrryxModActions {

    @KetherParser(["ghost"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionGhost() = combinationParser(
        Action.new("Orryx Mod额外功能", "设置鬼影状态", "ghost", true)
            .description("设置鬼影状态")
            .addEntry("时长", Type.LONG, false)
            .addEntry("密度", Type.INT)
            .addEntry("间隔", Type.INT, true, default = "0")
            .addContainerEntry(optional = true, default = "@self")
            .addContainerEntry(optional = true, default = "@self", head = "viewers", description = "可视玩家")
    ) {
        it.group(
            long(),
            int(),
            int().option().defaultsTo(0),
            theyContainer(true),
            command("viewers", then = container()).option()
        ).apply(it) { timeout, density, gap, container, viewers ->
            now {
                container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    viewers.orElse(self()).forEachInstance<PlayerTarget> { viewer ->
                        PluginMessageHandler.applyGhostEffect(viewer.getSource(), player.getSource(), timeout*50, density, gap)
                    }
                }
            }
        }
    }

    @KetherParser(["flicker"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFlicker() = combinationParser(
        Action.new("Orryx Mod额外功能", "滞留一道闪影", "flicker", true)
            .description("滞留一道闪影")
            .addEntry("时长", Type.LONG, false)
            .addEntry("透明度", Type.FLOAT, true, default = "1")
            .addContainerEntry(optional = true, default = "@self")
            .addContainerEntry(optional = true, default = "@self", head = "viewers", description = "可视玩家")
    ) {
        it.group(
            long(),
            float().option().defaultsTo(1f),
            theyContainer(true),
            command("viewers", then = container()).option()
        ).apply(it) { timeout, alpha, container, viewers ->
            now {
                container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    viewers.orElse(self()).forEachInstance<PlayerTarget> { viewer ->
                        PluginMessageHandler.applyFlickerEffect(viewer.getSource(), player.getSource(), timeout*50, alpha, -1L, 1.0f)
                    }
                }
            }
        }
    }

    @KetherParser(["mouse"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionMouse() = combinationParser(
        Action.new("Orryx Mod额外功能", "设置是否呼出鼠标", "mouse", true)
            .description("设置是否呼出鼠标")
            .addEntry("是否呼出", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            bool(),
            theyContainer(true)
        ).apply(it) { show, container ->
            now {
                container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    PluginMessageHandler.applyMouseCursor(player.getSource(), show)
                }
            }
        }
    }

    @KetherParser(["entityShow"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionEntityShow() = combinationParser(
        Action.new("Orryx Mod额外功能", "投影实体模型", "entityShow", true)
            .description("投影一个实体模型到指定位置")
            .addEntry("组名", Type.STRING, false)
            .addEntry("持续时间", Type.LONG, false)
            .addEntry("旋转", Type.STRING, true, "0,0,0")
            .addEntry("缩放", Type.FLOAT, true, "1.0")
            .addEntry("被投影的实体", Type.CONTAINER, false)
            .addContainerEntry(optional = true, default = "@world", head = "viewer")
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            text(),
            long(),
            command("rotate", then = text()).option().defaultsTo("0,0,0"),
            command("scale", then = float()).option().defaultsTo(1f),
            container(),
            command("viewer", then = container()).option(),
            theyContainer(true)
        ).apply(it) { group, timeout, rotate, scale, entity, viewer, container ->
            now {
                val (x, y, z) = rotate.split(",")
                val entities = entity.orElse(self()).get<ITargetEntity<*>>()
                val locations = container.orElse(self()).get<ITargetLocation<*>>()
                viewer.orElse(world()).forEachInstance<PlayerTarget> { player ->
                    entities.forEach { entity ->
                        locations.forEach { loc ->
                            PluginMessageHandler.applyEntityShowEffect(player.getSource(), entity.entity.uniqueId, group, loc.location, timeout, x.cfloat, y.cfloat, z.cfloat, scale)
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["removeShow"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionRemoveShow() = combinationParser(
        Action.new("Orryx Mod额外功能", "移除投影实体模型", "removeShow", true)
            .description("移除一个投影模型组")
            .addEntry("组名", Type.STRING, false)
            .addEntry("被投影的实体", Type.CONTAINER, false)
            .addContainerEntry(optional = true, default = "@world", head = "viewer")
    ) {
        it.group(
            text(),
            container(),
            command("viewer", then = container()).option()
        ).apply(it) { group, entity, viewer ->
            now {
                val entities = entity.orElse(self()).get<ITargetEntity<*>>()
                viewer.orElse(world()).forEachInstance<PlayerTarget> { player ->
                    entities.forEach { entity ->
                        PluginMessageHandler.removeEntityShowEffect(player.getSource(), entity.entity.uniqueId, group)
                    }
                }
            }
        }
    }

    @KetherParser(["navigation"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionNavigation() = scriptParser(
        Action.new("Orryx Mod额外功能", "开始一个自动寻路导航", "navigation", true)
            .description("开始一个自动寻路导航")
            .addEntry("开始占位符", Type.SYMBOL, false, head = "start")
            .addEntry("x", Type.INT, false)
            .addEntry("y", Type.INT, false)
            .addEntry("z", Type.INT, false)
            .addEntry("range", Type.INT, false),
        Action.new("Orryx Mod额外功能", "停止自动寻路导航", "navigation", true)
            .description("停止自动寻路导航")
            .addEntry("停止占位符", Type.SYMBOL, false, head = "stop")
    ) {
        it.switch {
            case("start") {
                val x = it.nextParsedAction()
                val y = it.nextParsedAction()
                val z = it.nextParsedAction()
                val r = it.nextParsedAction()
                actionNow {
                    run(x).int { x ->
                        run(y).int { y ->
                            run(z).int { z ->
                                run(r).int { r ->
                                    PluginMessageHandler.playerNavigation(script().bukkitPlayer(), x, y, z, r)
                                }
                            }
                        }
                    }
                }
            }
            case("stop") {
                actionNow {
                    PluginMessageHandler.stopPlayerNavigation(script().bukkitPlayer())
                }
            }
        }
    }
}