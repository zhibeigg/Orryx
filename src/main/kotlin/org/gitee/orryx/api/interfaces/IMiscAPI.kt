package org.gitee.orryx.api.interfaces

import org.gitee.orryx.module.experience.IExperience

/**
 * 杂项 API 接口
 *
 * 提供经验计算器等杂项功能的访问
 * */
interface IMiscAPI {

    /**
     * 获取经验计算器
     *
     * @param key 经验计算器键名
     * @return 经验计算器对象，如果不存在则返回 null
     * */
    fun getExperience(key: String): IExperience?
}