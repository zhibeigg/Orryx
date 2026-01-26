package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import java.util.concurrent.CompletableFuture
import java.util.function.Function

interface IKeyAPI {

    /**
     * 绑定技能到组的按键上
     *
     * 此方法会自动保存更改，无需手动调用保存方法
     *
     * @param skill 技能
     * @param group 组
     * @param bindKey 按键
     * @return 是否设置成功
     * */
    fun bindSkillKeyOfGroup(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): CompletableFuture<Boolean>

    /**
     * 绑定技能到组的按键上
     *
     * 此方法会自动保存更改，无需手动调用保存方法
     *
     * @param player 玩家
     * @param job 职业键名
     * @param skill 技能键名
     * @param group 组键名
     * @param bindKey 按键键名
     * @return 是否设置成功
     * */
    fun bindSkillKeyOfGroup(player: Player, job: String, skill: String, group: String, bindKey: String): CompletableFuture<Boolean>

    /**
     * 修改玩家按键设置
     *
     * 注意：此方法不会自动保存更改，如需持久化请在修改后手动调用 [PlayerKeySetting.save] 方法
     *
     * @param player 玩家
     * @param function 修改函数，接收 [PlayerKeySetting] 并返回结果
     * @return 修改函数的返回值
     * */
    fun <T> modifyKeySetting(player: Player, function: Function<PlayerKeySetting, T>): CompletableFuture<T>

    /**
     * 获得技能组
     *
     * @param key 技能组名
     * @return 技能组对象
     * @throws IllegalStateException 如果未找到指定的组
     * */
    fun getGroup(key: String): IGroup

    /**
     * 获得技能绑定键
     *
     * @param key 绑定键名
     * @return 绑定键对象
     * @throws IllegalStateException 如果未找到指定的绑定键
     * */
    fun getBindKey(key: String): IBindKey
}