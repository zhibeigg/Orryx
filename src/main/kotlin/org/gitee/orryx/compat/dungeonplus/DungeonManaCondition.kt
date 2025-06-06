package org.gitee.orryx.compat.dungeonplus

import org.gitee.orryx.module.mana.IManaManager
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
import taboolib.common5.cbool
import taboolib.module.chat.colored
import taboolib.module.kether.orNull
import taboolib.platform.compat.replacePlaceholder
import taboolib.platform.util.sendLang

@Ghost
@AutoRegister(dependPlugin = ["Orryx"], registerMessage = true)
class DungeonManaCondition : BasicActionScript(false) {

    override val type: Array<ScriptType> = arrayOf(ScriptType.SYSTEM)
    override val key: String = "o-mana-condition"
    override val mandatorySync: Boolean = false

    override val description: ActionScriptDescription = ActionScriptDescription("进入地牢蓝量限制")
        .type(*type)
        .sample("\$o-mana-condition{mana=>10;max=false;message=&4| &e%player_name% &f的蓝量不足 &c10} @system")
        .append("mana", "蓝量条件表达式", true)
        .append("max", "是否取最大蓝量", true)
        .append("message", "未满足后发送的信息", true)

    private lateinit var mana: RangedDouble
    private var max: Boolean = false
    private var message = "&4| &e%player_name% &f的蓝量不足 &c10"

    override fun init(dungeon: Dungeon, parameter: Map<String, String>): DungeonActionScript {
        mana = RangedDouble(parameter["mana"].toString())
        max = parameter["max"].cbool
        message = parameter["message"].toString()
        return this
    }

    override fun conditionScript(dungeon: Dungeon, scriptType: ScriptType): Boolean {
        var bypass = true
        val players = dungeon.team.getPlayers(PlayerStateType.ONLINE)
        players.forEach {
            val playerMana = if (max) {
                IManaManager.INSTANCE.getMaxMana(it).orNull() ?: 0
            } else {
                IManaManager.INSTANCE.getMana(it).orNull() ?: 0
            }
            if (!mana.equals(playerMana)) {
                bypass = false
                players.forEach { player ->
                    player.sendLang(message.replacePlaceholder(it).colored(), playerMana)
                }
            }
        }
        return bypass
    }
}