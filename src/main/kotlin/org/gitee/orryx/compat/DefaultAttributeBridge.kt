package org.gitee.orryx.compat

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.gitee.orryx.utils.doDamage

class DefaultAttributeBridge: IAttributeBridge {

    override fun addAttribute(key: String, value: List<String>) {
        TODO("Not yet implemented")
    }

    override fun removeAttribute(key: String) {
        TODO("Not yet implemented")
    }

    override fun damage(attacker: LivingEntity, target: LivingEntity, damage: Double) {
        doDamage(attacker, target, DamageCause.CUSTOM, damage)
    }

}