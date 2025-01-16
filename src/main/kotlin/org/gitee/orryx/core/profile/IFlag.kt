package org.gitee.orryx.core.profile

import java.io.Serializable

interface IFlag<T: Serializable> {

    val value: T

    val isPersistence: Boolean

    val timestamp: Long

    val timeout: Long

    fun isTimeout(): Boolean {
        return timeout + timestamp < System.currentTimeMillis()
    }

}