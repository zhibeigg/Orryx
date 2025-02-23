package org.gitee.orryx.core.skill.skills

interface IPress {

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