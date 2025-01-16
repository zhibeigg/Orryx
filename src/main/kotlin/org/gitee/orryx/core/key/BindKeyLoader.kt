package org.gitee.orryx.core.key

import taboolib.library.configuration.ConfigurationSection

class BindKeyLoader(override val key: String, val configurationSection: ConfigurationSection): IBindKey {

    override val sort: Int
        get() = configurationSection.getInt("sort")

}