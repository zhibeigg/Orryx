package org.gitee.orryx.core.skill.skills

/**
 * 当[aimMinAction]等于[aimMaxAction]时，将固定大小，否则将从min过渡到max
 * */
interface IAim {

    /**
     * 最小选区大小
     * */
    val aimMinAction: String

    /**
     * 最大选区大小
     * */
    val aimMaxAction: String

    /**
     * 选区中心距离玩家的最大半径
     * */
    val aimRadiusAction: String

}