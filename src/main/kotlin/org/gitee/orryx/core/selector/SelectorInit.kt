package org.gitee.orryx.core.selector

import org.bukkit.Bukkit
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.library.reflex.ReflexClass

@Awake
object SelectorInit: ClassVisitor(1) {

    private val selectors by lazy { mutableListOf<ISelector>() }

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.INIT
    }

    override fun visitStart(clazz: ReflexClass) {
        try {
            clazz.toClassOrNull()?.let { clazzClass ->
                if (ISelector::class.java.isAssignableFrom(clazzClass)) {
                    clazz.getInstance().let { instance ->
                        if (instance is ISelector) {
                            clazz.getAnnotationIfPresent(Plugin::class.java)?.let { annotation ->
                                val pluginEnabled = Bukkit.getPluginManager().isPluginEnabled(annotation.property<String>("plugin")!!)
                                debug("&e┣&7Selector loaded &e${instance.keys.map { it }} ${if (pluginEnabled) "&a√" else "&4×" }")
                                if (!pluginEnabled) return
                            } ?: run {
                                debug("&e┣&7Selector loaded &e${instance.keys.map { it }} &a√")
                            }
                            selectors.add(instance)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun getSelector(key: String): ISelector? {
        return selectors.firstOrNull { selector ->
            selector.keys.any { it.uppercase() == key.uppercase() }
        }
    }

}
