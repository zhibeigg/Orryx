package org.gitee.orryx.core.profile

sealed interface IFlag {

    val value: Any

    val isPersistence: Boolean

    val timestamp: Long

    val timeout: Long

    fun isTimeout(): Boolean {
        return  timeout != 0L && timeout + timestamp < System.currentTimeMillis()
    }

}