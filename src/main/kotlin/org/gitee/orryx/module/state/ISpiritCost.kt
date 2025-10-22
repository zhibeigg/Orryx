package org.gitee.orryx.module.state

import taboolib.library.configuration.ConfigurationSection

interface ISpiritCost {

    val configurationSection: ConfigurationSection

    /**
     * 消耗体力值
     * */
    val spirit: Double
}