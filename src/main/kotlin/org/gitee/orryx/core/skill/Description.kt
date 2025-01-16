package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.kether.ScriptManager.parseScript
import org.gitee.orryx.core.kether.parameter.SkillParameter
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender
import taboolib.module.chat.colored
import taboolib.module.kether.KetherFunction

class Description(val description: List<String>) {

    companion object {

        private val descriptionSplit: String
            get() = OrryxAPI.config.getString("DescriptionSplit", "&7→&r")!!

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
                KetherFunction.reader.replaceNested(it) {
                    val pre = parseScript(sender, skillParameter, this)
                    val next = parseScript(sender, skillParameter, this)
                    "$pre$descriptionSplit&e$next&r"
                }
            }
        }
        return list.colored()
    }

}