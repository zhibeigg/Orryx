package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext
import taboolib.platform.util.onlinePlayers

object Server: ISelectorStream {

    override val keys: Array<String> = arrayOf("server", "all", "players", "online")

    override val wiki: Selector
        get() = Selector.new("全服玩家", keys, SelectorType.STREAM)
            .addExample("@server")
            .addExample("@all")
            .addExample("@players")
            .addExample("@online")
            .description("获取全服在线玩家")

    override fun joinContainer(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        container.addAll(onlinePlayers.map { it.toTarget() })
    }

}