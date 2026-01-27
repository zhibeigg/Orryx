package org.gitee.orryx.core.job

/**
 * 职业配置接口。
 *
 * @property key 职业键名
 * @property name 职业显示名，默认采用键名
 * @property skills 职业技能列表
 * @property upgradePointActions 职业升级获取的技能点公式，`&level` 获取升级后的等级
 * @property maxManaActions 最大法力值公式
 * @property regainManaActions 自然恢复法力值公式
 * @property attributes 职业属性列表
 * @property experience 职业经验算法选择
 * @property maxSpiritActions 最大精力值公式
 * @property regainSpiritActions 自然恢复精力值公式
 */
interface IJob {

    val key: String

    val name: String

    val skills: List<String>

    val upgradePointActions: String

    val maxManaActions: String

    val regainManaActions: String

    val attributes: List<String>

    val experience: String

    val maxSpiritActions: String

    val regainSpiritActions: String
}
