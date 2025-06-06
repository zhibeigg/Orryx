package org.gitee.orryx.compat.dungeonplus

import org.gitee.orryx.utils.orryxProfile
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
class DungeonJobCondition : BasicActionScript(false) {

    override val type: Array<ScriptType> = arrayOf(ScriptType.SYSTEM)
    override val key: String = "o-job-condition"
    override val mandatorySync: Boolean = false

    override val description: ActionScriptDescription = ActionScriptDescription("进入地牢职业限制")
        .type(*type)
        .sample("\$o-job-condition{job=战士;message=&4| &e%player_name% &f的职业不是 &c战士} @system")
        .append("job", "职业", true)
        .append("message", "未满足后发送的信息", true)

    private var job: String = "战士"
    private var message = "&4| &e%player_name% &f的职业不是 &c战士"

    override fun init(dungeon: Dungeon, parameter: Map<String, String>): DungeonActionScript {
        job = parameter["job"].toString()
        message = parameter["message"].toString()
        return this
    }

    override fun conditionScript(dungeon: Dungeon, scriptType: ScriptType): Boolean {
        var bypass = true
        val players = dungeon.team.getPlayers(PlayerStateType.ONLINE)
        players.forEach {
            if (it.orryxProfile().orNull()?.job != job) {
                bypass = false
                players.forEach { player ->
                    player.sendLang(message.replacePlaceholder(it).colored(), job)
                }
            }
        }
        return bypass
    }
}