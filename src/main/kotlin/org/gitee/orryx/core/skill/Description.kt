package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.kether.ScriptManager.parseScript
import org.gitee.orryx.core.kether.ScriptManager.runScript
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.utils.ConfigLazy
import org.gitee.orryx.utils.ReloadableLazy
import org.gitee.orryx.utils.getFormatAtPosition
import org.gitee.orryx.utils.reader
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender
import taboolib.module.chat.colored
import taboolib.module.kether.orNull

class Description(val description: List<String>) {

    companion object {
        private val descriptionSplit: String by ConfigLazy(Orryx.config) { Orryx.config.getString("DescriptionSplit", "&7→")!! }
    }

    private fun getDescription(sender: ProxyCommandSender, skillParameter: SkillParameter): List<String> {
        return description.map { parseScript(sender, skillParameter, it.removePrefix("*")) }.colored()
    }

    fun getDescriptionComparison(player: Player, skillParameter: SkillParameter): List<String> {
        val sender = adaptCommandSender(player)
        if (skillParameter.level >= (skillParameter.getSkill()?.maxLevel ?: error("获取Description时未能读取到技能"))) return getDescription(sender, skillParameter)
        val list = mutableListOf<String>()
        description.forEach {
            list += if (it.startsWith("*")) {
                parseScript(sender, skillParameter, it.removePrefix("*"))
            } else {
                reader.replaceNested(it) { str, startPos ->
                    val pre = runScript(sender, skillParameter, str).orNull()
                    val next = runScript(sender, skillParameter.apply { level += 1 }, str).orNull()
                    "$pre$descriptionSplit&r${getFormatAtPosition(it.colored(), startPos)}$next&r"
                }
            }
        }
        return list.colored()
    }

}