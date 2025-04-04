package org.gitee.orryx.module.state

interface IRunningState {

    val state: IActionState

    fun start()

    fun stop()

}