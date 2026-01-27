package org.gitee.orryx.module.experience

import taboolib.common.platform.ProxyCommandSender

/**
 * 经验计算器接口。
 *
 * @property key 经验计算器的键名
 * @property minLevel 最低等级
 * @property maxLevel 最高等级
 * @property experienceEquation 经验算法，会前置变量 level，可使用 `&level` 取值
 */
interface IExperience {

    val key: String

    val minLevel: Int

    val maxLevel: Int

    val experienceEquation: String

    /**
     * 获取指定等级所需的经验值。
     *
     * @param sender 获取者
     * @param level 等级
     * @return 经验值
     */
    fun getExperienceOfLevel(sender: ProxyCommandSender, level: Int): Int
}
