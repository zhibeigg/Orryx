package org.gitee.orryx.api.events.register

import org.gitee.orryx.core.station.ITrigger
import taboolib.platform.type.BukkitProxyEvent

class OrryxTriggerRegisterEvent: BukkitProxyEvent() {

    override val allowCancelled: Boolean
        get() = false

    private val list = mutableListOf<ITrigger<*>>()

    fun <T> register(trigger: ITrigger<T>) {
        list += trigger
    }

    fun list(): List<ITrigger<*>> = list
}