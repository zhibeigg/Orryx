package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.ScriptManager.parseScript
import org.gitee.orryx.core.kether.parameter.SkillParameter
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender
import taboolib.module.chat.colored

class Icon(val icon: String) {

    private fun getIcon(sender: ProxyCommandSender, skillParameter: SkillParameter): String {
        return parseScript(sender, skillParameter, icon).colored()
    }

    fun getIcon(player: Player, skillParameter: SkillParameter): String {
        val sender = adaptCommandSender(player)
        return getIcon(sender, skillParameter)
    }
}