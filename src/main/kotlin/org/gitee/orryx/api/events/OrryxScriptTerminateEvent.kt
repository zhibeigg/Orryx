package org.gitee.orryx.api.events

import org.gitee.orryx.core.kether.RunningSpace
import taboolib.platform.type.BukkitProxyEvent

class OrryxScriptTerminateEvent {

    class Pre(val runningSpace: RunningSpace): BukkitProxyEvent()

    class Post(val runningSpace: RunningSpace): BukkitProxyEvent() {
        override val allowCancelled: Boolean
            get() = false
    }

}