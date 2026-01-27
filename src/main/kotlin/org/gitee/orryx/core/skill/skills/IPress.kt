package org.gitee.orryx.core.skill.skills

/**
 * 蓄力技能参数接口。
 *
 * @property pressBrockTriggers 蓄力会被打断的 Trigger
 * @property period 周期
 * @property pressPeriodAction 蓄力时每周期执行
 * @property maxPressTickAction 最大蓄力时间
 */
interface IPress {

    val pressBrockTriggers: Array<String>

    val period: Long

    val pressPeriodAction: String

    val maxPressTickAction: String
}
