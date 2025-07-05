package org.gitee.orryx.module.experience

import taboolib.common.platform.ProxyCommandSender

interface IExperience {

    /**
     * 经验计算器的键名
     * */
    val key: String

    /**
     * 最低等级
     * */
    val minLevel: Int

    /**
     * 最高等级
     * */
    val maxLevel: Int

    /**
     * 经验算法，会前置变量level，可以用&level取值
     * */
    val experienceEquation: String

    /**
     * 获取该等级下所需的经验值
     * @param sender 获取者
     * @param level 等级
     * @return 经验值
     * */
    fun getExperienceOfLevel(sender: ProxyCommandSender, level: Int): Int
}