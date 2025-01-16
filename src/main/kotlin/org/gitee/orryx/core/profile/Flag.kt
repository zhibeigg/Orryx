package org.gitee.orryx.core.profile

import java.io.Serializable

class Flag<T: Serializable>(override val value: T, override val isPersistence: Boolean, override val timeout: Long) : IFlag<T> {

    override val timestamp: Long = System.currentTimeMillis()

}