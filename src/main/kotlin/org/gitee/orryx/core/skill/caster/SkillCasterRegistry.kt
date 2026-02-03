package org.gitee.orryx.core.skill.caster

import org.gitee.orryx.core.skill.ISkill

/**
 * 技能释放器注册表。
 *
 * 管理所有技能释放器的注册和查找。
 */
object SkillCasterRegistry {

    private val casters = mutableListOf<ISkillCaster>()

    init {
        // 注册内置的技能释放器
        register(DirectSkillCaster)
        register(DirectAimSkillCaster)
        register(PressingSkillCaster)
        register(PressingAimSkillCaster)
    }

    /**
     * 注册一个技能释放器。
     *
     * @param caster 要注册的释放器
     */
    fun register(caster: ISkillCaster) {
        casters.add(caster)
    }

    /**
     * 获取支持指定技能的释放器。
     *
     * @param skill 要查找释放器的技能
     * @return 支持该技能的释放器，如果没有找到则返回 null
     */
    fun getCaster(skill: ISkill): ISkillCaster? {
        return casters.find { it.supports(skill) }
    }
}
