package org.gitee.orryx.core.selector.stream

import org.bukkit.Bukkit
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.ensureSync
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

object World: ISelectorStream {

    override val keys = arrayOf("world")

    override val wiki: Selector
        get() = Selector.new("添加/剔除世界目标", keys, SelectorType.STREAM)
            .addExample("@world sender/指定世界")
            .addExample("!@world sender/指定世界")
            .addParm(Type.STRING, "指定世界或者sender世界", "sender")
            .description("添加/剔除世界成员")

    override fun joinContainer(
        container: IContainer,
        context: ScriptContext,
        parameter: StringParser.Entry
    ) {
        val world = parameter.read(0, "sender")
        if (parameter.reverse) {
            if (world == "sender") {
                container.removeIf {
                    if (it is ITargetLocation<*>) {
                        it.world == context.bukkitPlayer().world
                    } else {
                        false
                    }
                }
            } else {
                container.removeIf {
                    if (it is ITargetLocation<*>) {
                        it.world.name == world
                    } else {
                        false
                    }
                }
            }
        } else {
            ensureSync {
                if (world == "sender") {
                    container.addAll(context.bukkitPlayer().world.livingEntities.map { it.toTarget() })
                } else {
                    val w = Bukkit.getWorld(world) ?: error("未找到世界 $world")
                    container.addAll(w.livingEntities.map { it.toTarget() })
                }
            }.join()
        }
    }

}