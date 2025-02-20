package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.SkillLevelResult

interface ISkillUI {

    /**
     * 查看UI的玩家
     * */
    val viewer: Player

    /**
     * 拥有UI的玩家
     * */
    val owner: Player

    /**
     * 打开UI
     * */
    fun open()

    /**
     * 更新HUD
     * */
    fun update()

    /**
     * 设置技能绑定按键
     * @param skill 技能
     * @param group 技能组
     * @param bindKey 绑定按键
     * @return 是否成功
     * */
    fun bindSkill(skill: String, group: String, bindKey: String): Boolean

    /**
     * 取消技能绑定
     * @param skill 技能
     * @param group 技能组
     * @return 是否成功
     * */
    fun unBindSkill(skill: String, group: String): Boolean

    /**
     * 升级技能
     * @param skill 技能
     * @return 结果
     * */
    fun upgrade(skill: String): SkillLevelResult

    /**
     * 重新分配技能点
     * @param skill 技能
     * @return 是否成功
     * */
    fun clearAndBackPoint(skill: String): Boolean

    /**
     * 重新分配技能点
     * @return 是否成功
     * */
    fun clearAllAndBackPoint(): Boolean

    /**
     * 获得玩家技能列表
     * @return 是否成功
     * */
    fun getSkills(): List<IPlayerSkill>

    /**
     * 获得玩家绑定技能
     * @param group 技能组
     * @return 是否成功
     * */
    fun getGroupSkills(group: String): Map<IBindKey, String?>

}