package org.gitee.orryx.core.selector.stream

import org.bukkit.Bukkit
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.state.StateManager.statusData
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

@Plugin("DragonCore")
object Joiner: ISelectorStream {

    override val keys = arrayOf("joiner")

    override val wiki: Selector
        get() = Selector.new("进入sender客户端的实体", keys, SelectorType.STREAM)
            .addExample("@joiner")
            .addExample("!@joiner")
            .description("进入sender客户端的实体")

    override fun joinContainer(
        container: IContainer,
        context: ScriptContext,
        parameter: StringParser.Entry
    ) {
        if (parameter.reverse) {
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    it.entity.uniqueId in context.bukkitPlayer().statusData().cacheJoiner
                } else {
                    false
                }
            }
        } else {
            container.addAll(context.bukkitPlayer().statusData().cacheJoiner.mapNotNull { Bukkit.getPlayer(it)?.toTarget() })
        }
    }

}