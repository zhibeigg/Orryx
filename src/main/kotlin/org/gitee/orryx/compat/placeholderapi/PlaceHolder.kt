package org.gitee.orryx.compat.placeholderapi

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.kether.ScriptManager.runKether
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptService
import taboolib.module.kether.orNull
import taboolib.platform.compat.PlaceholderExpansion
import java.util.concurrent.CompletableFuture

object PlaceHolder: PlaceholderExpansion {

    private val scriptsMap = hashMapOf<String, Script>()

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        scriptsMap.clear()
        files("placeholders", "example.yml") {
            val config = Configuration.loadFromFile(it)
            config.getKeys(false).forEach { key ->
                config.getString(key)?.let { action ->
                    scriptsMap[key] = loadScript(key, action) ?: return@forEach
                }
            }
        }
        consoleMessage("&e┣&7PlaceHolders loaded &e${scriptsMap.size} &a√")
    }

    private fun loadScript(key: String, action: String): Script? {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, "placeholder@$key", getBytes(action), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            warning("Placeholder: $key 加载失败")
            ex.printKetherErrorMessage()
            null
        }
    }

    override val identifier: String
        get() = "orryx"

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        return runKether(CompletableFuture.completedFuture(null)) {
            ScriptContext.create(scriptsMap[args]!!).also {
                it.sender = adaptCommandSender(player ?: Bukkit.getConsoleSender())
                it.id = NanoId.generate()
            }.runActions()
        }.orNull().toString()
    }
}