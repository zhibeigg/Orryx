package org.gitee.orryx.core.damage

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.utils.apEvent
import org.gitee.orryx.utils.isAttributePlus
import org.gitee.orryx.utils.isOriginAttribute
import org.gitee.orryx.utils.oaDamageEvent
import taboolib.platform.type.BukkitProxyEvent

abstract class AbstractDamageEvent(
    val attacker: Entity,
    val victim: Entity,
    private var privateDamage: Double,
    val event: EntityDamageByEntityEvent?,
    var type: DamageType
) : BukkitProxyEvent() {

    val data = mutableMapOf<String, Any>()

    val damage: Double
        get() = when {
            isOriginAttribute() -> oaDamageEvent()!!.damageMemory.totalDamage
            isAttributePlus() -> apEvent()!!.damage
            else -> privateDamage
        }

    fun addDamage(damage: Double) {
        if (damage < 0) takeDamage(-damage)
        when {
            isOriginAttribute() -> oaDamageEvent()!!.damageMemory.addDamage(java.util.UUID.randomUUID().toString(), damage)
            isAttributePlus() -> apEvent()!!.damage += damage
            else -> privateDamage += damage
        }
    }

    fun takeDamage(damage: Double) {
        if (damage < 0) addDamage(-damage)
        when {
            isOriginAttribute() -> oaDamageEvent()!!.damageMemory.takeDamage(java.util.UUID.randomUUID().toString(), damage)
            isAttributePlus() -> apEvent()!!.damage -= damage
            else -> privateDamage -= damage
        }
    }

    fun attackPlayer(): Player? {
        if (attacker is Projectile) {
            return attacker.shooter as? Player ?: return null
        }
        return attacker as? Player
    }

    fun victimPlayer(): Player? {
        return victim as? Player
    }

}