package org.gitee.orryx.core.skill.skills

/**
 * 技能瞄准参数接口。
 *
 * 当 [aimMinAction] 等于 [aimMaxAction] 时，将固定大小，否则将从 min 过渡到 max。
 *
 * @property aimMinAction 最小选区大小
 * @property aimMaxAction 最大选区大小
 * @property aimRadiusAction 选区中心距离玩家的最大半径
 */
interface IAim {

    val aimMinAction: String

    val aimMaxAction: String

    val aimRadiusAction: String
}
