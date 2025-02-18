package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.platform.util.sendLang

enum class CastResult {

    SUCCESS,PARAMETER,MANA_NOT_ENOUGH,CHECK_ACTION_FAILED,COOLDOWN,CANCELED,PASSIVE;

    fun sendLang(player: Player) {
        when(this) {
            SUCCESS -> {}
            PARAMETER -> warning("识别到技能释放时使用了错误的参数")
            MANA_NOT_ENOUGH -> player.sendLang("mana-not-enough")
            CHECK_ACTION_FAILED -> player.sendLang("check-action-failed")
            COOLDOWN -> player.sendLang("cooldown")
            CANCELED -> player.sendLang("canceled")
            PASSIVE -> warning("请勿尝试释放被动技能")
        }
    }

}