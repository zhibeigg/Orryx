package org.gitee.orryx.utils

import org.gitee.orryx.core.profile.Flag
import org.gitee.orryx.core.profile.IFlag
import java.io.Serializable

inline fun <reified T : Serializable> T.flag(isPersistence: Boolean = false, timeout: Long = 0): IFlag<*> {
    return Flag(this, isPersistence, timeout)
}