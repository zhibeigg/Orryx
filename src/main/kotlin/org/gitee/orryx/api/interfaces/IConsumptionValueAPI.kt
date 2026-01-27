package org.gitee.orryx.api.interfaces

import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.spirit.ISpiritManager

/**
 * 消耗值 API 接口。
 *
 * 提供对法力值和精力值管理器的访问。
 *
 * @property manaInstance 法力值管理器实例，用于管理玩家的法力值
 * @property spiritInstance 精力值管理器实例，用于管理玩家的精力值
 */
interface IConsumptionValueAPI {

    val manaInstance: IManaManager

    val spiritInstance: ISpiritManager
}
