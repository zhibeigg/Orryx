package org.gitee.orryx.module.lang

import me.goudan.gddtitle.api.GDDTitleAPI
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.util.replaceWithOrder
import taboolib.module.lang.Type

class TypeGDDTitleTitle : Type {

    lateinit var text: String

    override fun init(source: Map<String, Any>) {
        text = source["text"].toString()
    }

    override fun send(sender: ProxyCommandSender, vararg args: Any) {
        val newText = text.translate(sender, *args).replaceWithOrder(*args)
        if (sender is ProxyPlayer) {
            GDDTitleAPI.sendTitle(sender.cast(), newText)
        } else {
            sender.sendMessage(toString())
        }
    }

    override fun toString(): String {
        return "NodeGDDTitle(text='$text')"
    }
}