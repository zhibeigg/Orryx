package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import java.util.concurrent.CompletableFuture

interface IKeyAPI {

    /**
     * 绑定技能到组的按键上
     * @param skill 技能
     * @param group 组
     * @param bindKey 按键
     * @return 是否设置成功
     * */
    fun bindSkillKeyOfGroup(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): CompletableFuture<Boolean>

    /**
     * 绑定技能到组的按键上
     * @param player 玩家
     * @param skill 技能键名
     * @param group 组键名
     * @param bindKey 按键键名
     * @return 是否设置成功
     * */
    fun bindSkillKeyOfGroup(player: Player, job: String, skill: String, group: String, bindKey: String): CompletableFuture<Boolean>

    /**
     * 获得技能组
     * @param key 技能组名
     * */
    fun getGroup(key: String): IGroup

    /**
     * 获得技能绑定键
     * @param key 绑定键名
     * */
    fun getBindKey(key: String): IBindKey

}