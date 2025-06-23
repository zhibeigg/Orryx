package org.gitee.orryx.api.events

import org.gitee.orryx.core.profile.IFlag
import taboolib.platform.type.BukkitProxyEvent

class OrryxGlobalFlagChangeEvents {

    class Pre(var flagName: String, val oldFlag: IFlag?, var newFlag: IFlag?): BukkitProxyEvent()

    class Post(val flagName: String, val oldFlag: IFlag?, val newFlag: IFlag?): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}