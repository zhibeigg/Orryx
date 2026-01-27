package org.gitee.orryx.api.interfaces

/**
 * Orryx 插件主 API 接口。
 *
 * 提供对所有子 API 的访问入口。
 *
 * @property keyAPI 按键相关 API，用于管理技能按键绑定和玩家按键设置
 * @property reloadAPI 重载 API，用于重载插件配置
 * @property timerAPI 计时器 API，用于获取技能计时器和中转站计时器
 * @property profileAPI 玩家档案 API，用于管理玩家状态（霸体、无敌、格挡、沉默等）
 * @property jobAPI 职业 API，用于获取和修改玩家职业数据
 * @property skillAPI 技能 API，用于获取、修改和释放技能
 * @property taskAPI 任务 API，用于创建和管理简单任务和管道任务
 * @property consumptionValueAPI 消耗值 API，用于获取法力值和精力值管理器
 * @property miscAPI 杂项 API，用于获取经验计算器等杂项功能
 */
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
