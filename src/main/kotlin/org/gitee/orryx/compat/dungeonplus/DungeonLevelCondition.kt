package org.gitee.orryx.compat.dungeonplus

import org.gitee.orryx.utils.RangedDouble
import org.gitee.orryx.utils.job
import org.serverct.ersha.dungeon.common.api.annotation.AutoRegister
import org.serverct.ersha.dungeon.common.api.component.script.ActionScriptDescription
import org.serverct.ersha.dungeon.common.api.component.script.BasicActionScript
import org.serverct.ersha.dungeon.common.api.component.script.DungeonActionScript
import org.serverct.ersha.dungeon.common.api.component.script.type.ScriptType
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType
import org.serverct.ersha.dungeon.internal.dungeon.Dungeon
import taboolib.common.platform.Ghost
import taboolib.module.chat.colored
import taboolib.module.kether.orNull
import taboolib.platform.compat.replacePlaceholder
import taboolib.platform.util.sendLang

@Ghost
@AutoRegister(dependPlugin = ["Orryx"], registerMessage = true)
class DungeonLevelCondition : BasicActionScript(false) {

    override val type: Array<ScriptType> = arrayOf(ScriptType.SYSTEM)
    override val key: String = "o-level-condition"
    override val mandatorySync: Boolean = false

    override val description: ActionScriptDescription = ActionScriptDescription("进入地牢等级限制")
        .type(*type)
        .sample("\$o-level-condition{level=>10;message=&4| &e%player_name% &f的等级不足 &c10 &f级} @system")
        .append("level", "等级条件表达式", true)
        .append("message", "未满足后发送的信息", true)

    private lateinit var level: RangedDouble
    private var message = "&4| &e%player_name% &f的等级不足 &c10 &f级"

    override fun init(dungeon: Dungeon, parameter: Map<String, String>): DungeonActionScript {
        level = RangedDouble(parameter["level"].toString())
        message = parameter["message"].toString()
        return this
    }

    override fun conditionScript(dungeon: Dungeon, scriptType: ScriptType): Boolean {
        var bypass = true
        val players = dungeon.team.getPlayers(PlayerStateType.ONLINE)
        players.forEach {
            val playerLevel = it.job().orNull()?.level ?: 0
            if (!level.equals(playerLevel)) {
                bypass = false
                players.forEach { player ->
                    player.sendLang(message.replacePlaceholder(it).colored(), playerLevel)
                }
            }
        }
        return bypass
    }
}