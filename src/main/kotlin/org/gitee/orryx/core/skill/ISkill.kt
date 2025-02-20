package org.gitee.orryx.core.skill

interface ISkill {

    /**
     * 技能键名
     * */
    val key: String

    /**
     * 技能显示名
     * */
    val name: String

    /**
     * 技能显示图标名
     * */
    val icon: String

    /**
     * 技能类型
     * */
    val type: String

    /**
     * 技能是否需要解锁
     * */
    val isLocked: Boolean

    /**
     * 技能解锁后的最低等级
     * */
    val minLevel: Int

    /**
     * 技能最高等级
     * */
    val maxLevel: Int

    /**
     * 技能介绍
     * */
    val description: Description

    /**
     * 技能升级消耗的技能点
     * */
    val upgradePointAction: String?

    /**
     * 技能升级前检测
     * */
    val upLevelCheckAction: String?

    /**
     * 技能升级成功执行
     * */
    val upLevelSuccessAction: String?

    /**
     * 技能的延迟生成变量
     * */
    val variables: Map<String, String>


}