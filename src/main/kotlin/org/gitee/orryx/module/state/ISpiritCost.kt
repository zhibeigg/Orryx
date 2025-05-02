package org.gitee.orryx.module.state

import taboolib.library.configuration.ConfigurationSection

interface ISpiritCost {

    val configurationSection: ConfigurationSection

    val spirit: Double
}