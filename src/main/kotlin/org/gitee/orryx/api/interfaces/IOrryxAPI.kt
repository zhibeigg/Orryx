package org.gitee.orryx.api.interfaces

interface IOrryxAPI {

    val keyAPI: IKeyAPI

    val reloadAPI: IReloadAPI

    val timerAPI: ITimerAPI

    val profileAPI: IProfileAPI

    val jobAPI: IJobAPI

    val skillAPI: ISkillAPI

    val taskAPI: ITaskAPI

    val consumptionValueAPI: IConsumptionValueAPI

    val miscAPI: IMiscAPI
}