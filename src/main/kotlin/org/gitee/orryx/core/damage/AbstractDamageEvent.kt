package org.gitee.orryx.core.damage

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.utils.apEvent
import org.gitee.orryx.utils.isAttributePlus
import taboolib.module.kether.ScriptContext
import taboolib.platform.type.BukkitProxyEvent

abstract class AbstractDamageEvent(
    val attacker: Entity,
    val defender: Entity,
    private var privateDamage: Double,
    val event: EntityDamageByEntityEvent?,
    var type: DamageType,
    val context: ScriptContext? = null
) : BukkitProxyEvent() {

    internal var origin: Any? = null

    val damage: Double
        get() = when {
            isAttributePlus() -> apEvent()!!.damage
            else -> privateDamage
        }

    fun addDamage(damage: Double) {
        if (damage < 0) takeDamage(-damage)
        when {
            isAttributePlus() -> apEvent()!!.damage += damage
            else -> privateDamage += damage
        }
    }

    fun takeDamage(damage: Double) {
        if (damage < 0) addDamage(-damage)
        when {
            isAttributePlus() -> apEvent()!!.damage -= damage
            else -> privateDamage -= damage
        }
    }

    fun attackPlayer(): Player? {
        if (attacker is Projectile) {
            return attacker.shooter as? Player
        }
        return attacker as? Player
    }

    fun defenderPlayer(): Player? {
        return defender as? Player
    }
}