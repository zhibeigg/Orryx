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
class DungeonFlagCondition : BasicActionScript(false) {

    override val type: Array<ScriptType> = arrayOf(ScriptType.SYSTEM)
    override val key: String = "o-flag-condition"
    override val mandatorySync: Boolean = false

    override val description: ActionScriptDescription = ActionScriptDescription("进入地牢Flag限制")
        .type(*type)
        .sample("\$o-flag-condition{key=阳虚状态;value=false;message=&4| &e%player_name% &f处于阳虚中，无法进入回阳境} @system")
        .append("key", "键", true)
        .append("value", "值", true)
        .append("message", "未满足后发送的信息", true)

    private var flag: String = "阳虚状态"
    private var value: String = "false"
    private var message = "&4| &e%player_name% &f的职业不是 &c战士"

    override fun init(dungeon: Dungeon, parameter: Map<String, String>): DungeonActionScript {
        flag = parameter["flag"] ?: parameter["key"] ?: flag
        value = parameter["value"] ?: value
        message = parameter["message"] ?: message
        return this
    }

    override fun conditionScript(dungeon: Dungeon, scriptType: ScriptType): Boolean {
        var bypass = true
        dungeon.team.getPlayers(PlayerStateType.ONLINE).forEach { player ->
            val flagValue = player.orryxProfile().orNull()?.flags?.get(flag)?.value?.toString()
            if (flagValue != value) {
                bypass = false
                player.sendLang(message.replacePlaceholder(player).colored(), flagValue.orEmpty())
            }
        }
        return bypass
    }
}