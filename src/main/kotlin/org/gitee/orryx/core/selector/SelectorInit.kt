package org.gitee.orryx.core.selector

import org.bukkit.Bukkit
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.ReflexClass

@Awake
object SelectorInit: ClassVisitor(3) {

    private val selectors by unsafeLazy { mutableListOf<ISelector>() }

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.LOAD
    }

    override fun visitStart(clazz: ReflexClass) {
        if (ISelector::class.java.isAssignableFrom(clazz.toClass())) {
            clazz.getInstance().let { instance ->
                if (instance is ISelector) {
                    if (instance is WikiSelector) {
                        ScriptManager.wikiSelectors += instance.wiki
                    }
                    clazz.getAnnotationIfPresent(Plugin::class.java)?.let { annotation ->
                        val pluginLoaded = Bukkit.getPluginManager().getPlugin(annotation.property<String>("plugin")!!) != null
                        debug { "&e┣&7Selector loaded &e${instance.keys.map { it }} ${if (pluginLoaded) "&a√" else "&4×"}" }
                        if (!pluginLoaded) return
                    } ?: run {
                        debug { "&e┣&7Selector loaded &e${instance.keys.map { it }} &a√" }
                    }
                    selectors.add(instance)
                }
            }
        }
    }

    fun getSelector(key: String): ISelector? {
        return selectors.firstOrNull { selector ->
            selector.keys.any { it.uppercase() == key.uppercase() }
        }
    }

}
