package org.gitee.orryx.core.skill.skills

interface IPress {

    /**
     * 蓄力会被打断的Trigger
     * */
    val pressBrockTriggers: Array<String>

    /**
     * 周期
     */
    val period: Long

    /**
     * 蓄力时每周期执行
     */
    val pressPeriodAction: String

    /**
     * 最大蓄力时间
     */
    val maxPressTickAction: String
}