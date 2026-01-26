package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.ISkill
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * 技能 API 接口
 *
 * 提供对技能的获取、修改和释放功能
 * */
interface ISkillAPI {

    /**
     * 获取玩家职业技能并进行操作
     *
     * 注意：此方法不会自动保存更改，如需持久化请在修改后手动调用 [IPlayerSkill.save] 方法
     *
     * @param player 玩家
     * @param skill 技能键名
     * @param job 职业，为 null 时使用玩家当前职业
     * @param function 获取到技能后执行的函数
     * @return [function] 的返回值，如果玩家没有该技能则返回 null
     * */
    fun <T> modifySkill(player: Player, skill: String, job: IPlayerJob? = null, function: Function<IPlayerSkill, T>) : CompletableFuture<T?>

    /**
     * 直接释放技能
     *
     * 此方法会直接释放技能，不进行任何条件检查（如冷却、法力值等）
     *
     * @param player 玩家
     * @param skill 技能键名
     * @param level 技能等级
     * */
    fun castSkill(player: Player, skill: String, level: Int)

    /**
     * 尝试释放技能
     *
     * 此方法会进行完整的条件检查（冷却、法力值、沉默状态等），满足条件后释放技能
     *
     * @param player 玩家
     * @param skill 技能键名
     * @return 释放结果，如果玩家没有该技能则返回 null
     * */
    fun tryCastSkill(player: Player, skill: String): CompletableFuture<CastResult?>

    /**
     * 获取技能配置
     *
     * @param skill 技能键名
     * @return 技能配置对象，如果不存在则返回 null
     * */
    fun getSkill(skill: String): ISkill?
}