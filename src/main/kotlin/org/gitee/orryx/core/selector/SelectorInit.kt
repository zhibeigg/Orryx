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

    private val selectors by lazy {  mutableListOf<ISelector>() }

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.INIT
    }

    override fun visitStart(clazz: ReflexClass) {
        try {
            if (ISelector::class.java.isAssignableFrom(clazz.toClass())) {
                val instance = clazz.getInstance() as? ISelector ?: return
                if (clazz.hasAnnotation(Plugin::class.java)) {
                    val annotation = clazz.getAnnotation(Plugin::class.java)
                    val pluginEnabled = Bukkit.getPluginManager().isPluginEnabled(annotation.property<String>("plugin")!!)
                    debug("&e┣&7Selector loaded &e${instance.keys.map { it }} ${if (pluginEnabled) "&a√" else "&4×" }")
                    if (!pluginEnabled) return
                } else {
                    debug("&e┣&7Selector loaded &e${instance.keys.map { it }} &a√")
                }
                selectors.add(instance)
            }
        } catch (_: Throwable) {
        }
    }

    fun getSelector(key: String): ISelector? {
        return selectors.firstOrNull {
            key in it.keys.map { v -> v.uppercase() }
        }
    }

}