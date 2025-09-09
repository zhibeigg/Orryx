package org.gitee.orryx.compat.astraxhero

import cn.bukkitmc.hero.AstraXHero
import cn.bukkitmc.hero.AttributeAPI.addSourAttr
import cn.bukkitmc.hero.AttributeAPI.takeSourAttr
import cn.bukkitmc.hero.FightAPI
import cn.bukkitmc.hero.module.fight.FightData
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.compat.IAttributeBridge
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import taboolib.module.kether.ScriptContext

class AstraXHeroBridge : IAttributeBridge {
    override fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long) {
        entity.addSourAttr(key, value)
        update(entity)
        if (timeout != -1L) {
            SimpleTimeoutTask.createSimpleTask(timeout) {
                removeAttribute(entity, key)
            }
        }
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        entity.takeSourAttr(key)
    }

    // 与 axhDamage 语句的区别是无法自定义变量
    // data 会携带实体属性，最终伤害需要在 AstraXHero 的属性触发器中计算
    override fun damage(
        attacker: LivingEntity,
        target: LivingEntity,
        damage: Double,
        type: DamageType,
        context: ScriptContext?
    ) {
        val data = FightData(attacker, target) {
            it["orryx"] = true
            it["orryx-damage"] = damage
            it["orryx-type"] = type
            context?.let { _ -> it["orryx-context"] = context }
        }
        FightAPI.runFight(data, true)
    }

    override fun update(entity: LivingEntity) {
        AstraXHero.astraXHeroAPI.update(entity)
    }
}