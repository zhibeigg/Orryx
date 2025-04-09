package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.ISkill
import java.util.concurrent.CompletableFuture

interface ISkillAPI {

    /**
     * 获取玩家职业技能并进行操作
     *
     * @param player 玩家
     * @param skill 技能
     * @param job 职业(默认当前职业)
     * @param function 获取到职业后执行
     * @return [function]的返回值
     * */
    fun <T> modifySkill(player: Player, skill: String, job: IPlayerJob? = null, function: (skill: IPlayerSkill) -> T) : CompletableFuture<T?>

    fun castSkill(player: Player, skill: String, level: Int)

    fun tryCastSkill(player: Player, skill: String): CompletableFuture<CastResult?>

    fun getSkill(skill: String): ISkill?

}