package org.gitee.orryx.core.selector.filter

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorFilter
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.utils.bukkitPlayer
import org.serverct.ersha.dungeon.DungeonPlus
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType
import taboolib.module.kether.ScriptContext

/**
 * 只保留队内人员,或只保留队外人员
 * ```
 * @team
 * !@team
 * ```
 * */
object Team: ISelectorFilter {

    override val keys: Array<String>
        get() = arrayOf("team")

    override fun filter(target: ITarget<*>, context: ScriptContext, parameter: StringParser.Entry): Boolean {
        val teamPlayers = DungeonPlus.teamManager.getTeam(context.bukkitPlayer())?.getPlayers(PlayerStateType.ONLINE) ?: return true
        val boolean = target is PlayerTarget && teamPlayers.contains(target.player)
        return if (parameter.reverse) {
            !boolean
        } else {
            boolean
        }
    }

}