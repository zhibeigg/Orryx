package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.toTarget
import org.serverct.ersha.dungeon.DungeonPlus
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType
import taboolib.module.kether.ScriptContext

/**
 * 添加队员
 * ```
 * @teammate
 * ```
 * */
@Plugin("DungeonPlus")
object Teammate: ISelectorStream {

    override val keys = arrayOf("teammate")

    override fun joinContainer(
        container: IContainer,
        context: ScriptContext,
        parameter: StringParser.Entry
    ) {
        val teamPlayers = DungeonPlus.teamManager.getTeam(context.bukkitPlayer())?.getPlayers(PlayerStateType.ONLINE)
        teamPlayers?.let { players -> container.addAll(players.map { it.toTarget() }) }
    }

}