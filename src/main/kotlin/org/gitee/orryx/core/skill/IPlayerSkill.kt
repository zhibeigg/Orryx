package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup

interface IPlayerSkill {

    /**
     * 拥有玩家
     * */
    val player: Player

    /**
     * 技能键名
     * */
    val key: String

    /**
     * 技能配置文件
     * */
    val skill: ISkill

    /**
     * 技能所属职业
     * */
    val job: String

    /**
     * 技能是否锁定
     * */
    val locked: Boolean

    /**
     * 技能等级
     * */
    val level: Int

    /**
     * 绑定的组和按键
     * */
    val bindKeyOfGroup: Map<String, String?>

    /**
     * 强制的释放技能
     *
     * 不会经过任何检测，同时Cast事件也在此call
     * @param parameter 释放参数
     * @return 释放结果
     * */
    fun cast(parameter: IParameter): CastResult

    /**
     * 释放技能检测
     *
     * 会经过多道检测，同时Check事件也在此call
     * @param parameter 释放参数
     * @return 释放结果
     * */
    fun castCheck(parameter: IParameter): CastResult

    /**
     * 升级检测
     * @param from 从等级
     * @param to 到等级
     * @return 是否可以升级
     * */
    fun upLevelCheck(from: Int, to: Int): Boolean

    /**
     * 升级成功执行
     * @param from 从等级
     * @param to 到等级
     * */
    fun upLevelSuccess(from: Int, to: Int)

    /**
     * 升级技能
     * @param level 等级
     * @return 是否成功
     * */
    fun upLevel(level: Int): SkillLevelResult

    /**
     * 降级技能
     * @param level 等级
     * @return 是否成功
     * */
    fun downLevel(level: Int): SkillLevelResult

    /**
     * 设置技能等级
     * @param level 等级
     * @return 是否成功
     * */
    fun setLevel(level: Int): SkillLevelResult

    /**
     * 设置技能绑定按键
     * @param group 技能组
     * @param bindKey 绑定按键
     * @return 是否成功
     * */
    fun setBindKey(group: IGroup, bindKey: IBindKey): Boolean

    /**
     * 保存数据
     * @param async 是否异步
     * */
    fun save(async: Boolean = true)

}