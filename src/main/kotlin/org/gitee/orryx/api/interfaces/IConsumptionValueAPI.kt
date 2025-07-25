package org.gitee.orryx.api.interfaces

import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.spirit.ISpiritManager

interface IConsumptionValueAPI {

    val manaInstance: IManaManager

    val spiritInstance: ISpiritManager
}