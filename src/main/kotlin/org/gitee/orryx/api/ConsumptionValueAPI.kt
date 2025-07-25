package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.IConsumptionValueAPI
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.spirit.ISpiritManager
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

class ConsumptionValueAPI: IConsumptionValueAPI {

    override val manaInstance: IManaManager
        get() = IManaManager.INSTANCE

    override val spiritInstance: ISpiritManager
        get() = ISpiritManager.INSTANCE

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<IConsumptionValueAPI>(ConsumptionValueAPI())
        }
    }
}