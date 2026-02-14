package org.gitee.orryx.core.skill

/**
 * 技能配置接口。
 *
 * @property key 技能键名
 * @property name 技能显示名
 * @property sort UI 显示顺序
 * @property icon 技能显示图标名
 * @property xMaterial 技能显示材质
 * @property type 技能类型
 * @property isLocked 技能是否需要解锁
 * @property minLevel 解锁后的最低等级
 * @property maxLevel 最高等级
 * @property description 技能介绍
 * @property upgradePointAction 技能升级消耗的技能点
 * @property upLevelCheckAction 技能升级前检测
 * @property downLevelCheckAction 技能降级前检测
 * @property upLevelSuccessAction 技能升级成功执行
 * @property downLevelSuccessAction 技能降级成功执行
 * @property ignoreSilence 是否无视沉默
 * @property variables 技能的延迟生成变量
 */
interface ISkill {

    val key: String

    val name: String

    val sort: Int

    val icon: Icon

    val xMaterial: String

    val type: String

    val isLocked: Boolean

    val minLevel: Int

    val maxLevel: Int

    val description: Description

    val upgradePointAction: String?

    val upLevelCheckAction: String?

    val downLevelCheckAction: String?

    val upLevelSuccessAction: String?

    val downLevelSuccessAction: String?

    val ignoreSilence: Boolean

    val variables: Map<String, String>
}
