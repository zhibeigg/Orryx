package org.gitee.orryx.core.job

interface IJob {

    /**
     * 职业键名
     * */
    val key: String

    /**
     * 职业显示名
     * 默认采用键名
     * */
    val name: String

    /**
     * 职业技能列表
     * */
    val skills: List<String>

    /**
     * 职业升级获取的技能点
     * &level获取升级到的等级
     * */
    val upgradePointActions: String

    /**
     * 职业升级获取的技能点
     * &level获取升级到的等级
     * */
    val maxManaActions: String

    /**
     * 职业恢复法力的值
     * */
    val regainManaActions: String

    /**
     * 职业获取的属性值
     * */
    val attributes: List<String>

    /**
     * 职业的经验算法选择
     * */
    val experience: String
}