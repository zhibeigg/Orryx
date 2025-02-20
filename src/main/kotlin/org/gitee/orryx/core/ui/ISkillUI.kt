package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill

interface ISkillUI {

    val viewer: Player

    /**
     * 设置技能绑定按键
     * @param skill 技能
     * @param group 技能组
     * @param bindKey 绑定按键
     * @return 是否成功
     * */
    fun bindSkill(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): Boolean

}