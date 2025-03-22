package org.gitee.orryx.api.events

import taboolib.common.util.unsafeLazy
import taboolib.platform.type.BukkitProxyEvent

class OrryxPluginReloadEvent: BukkitProxyEvent() {

    private val functions by unsafeLazy { mutableListOf<Func>() }

    class Func(val weight: Int, private val function: Runnable) {
        fun run() {
            function.run()
        }
    }

    fun registerFunction(weight: Int, function: Runnable) {
        functions.add(Func(weight, function))
    }

    internal fun getFunctions(): List<Func> {
        return functions
    }

}