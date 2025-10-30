package org.gitee.orryx.core.key

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.DEFAULT
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.info
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object BindKeyLoaderManager {

    @Config("keys.yml")
    lateinit var keys: ConfigFile
        private set

    private lateinit var bindKeyLoaderMap: Map<String, IBindKey>
    private lateinit var groupMap: Map<String, IGroup>

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        keys.reload()
        bindKeyLoaderMap = keys.getConfigurationSection("Keys")?.getKeys(false)?.associate {
            it.uppercase() to BindKeyLoader(it.uppercase(), keys.getConfigurationSection("Keys.$it")!!)
        } ?: emptyMap()
        groupMap = (Orryx.config.getStringList("Group") + DEFAULT).associateWith { GroupLoader(it) }
        consoleMessage("&e┣&7Groups loaded &e${groupMap.size} &a√")
        consoleMessage("&e┣&7BindKeys loaded &e${bindKeyLoaderMap.size} &a√")
    }

    internal fun getGroup(key: String): IGroup? = groupMap[key]

    internal fun getBindKey(key: String): IBindKey? = bindKeyLoaderMap[key]

    internal fun getGroups(): Map<String, IGroup> = groupMap

    internal fun getBindKeys(): Map<String, IBindKey> = bindKeyLoaderMap
}