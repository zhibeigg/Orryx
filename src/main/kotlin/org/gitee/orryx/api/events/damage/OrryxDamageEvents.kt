package org.gitee.orryx.api.events.damage

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.orryx.core.damage.AbstractDamageEvent
import taboolib.module.kether.ScriptContext
import taboolib.platform.type.BukkitProxyEvent

class OrryxDamageEvents {

    class Pre(attacker: Entity, defender: Entity, privateDamage: Double, event: EntityDamageByEntityEvent?, type: DamageType, context: ScriptContext? = null): AbstractDamageEvent(attacker, defender, privateDamage, event, type, context)

    class Post(val attacker: Entity, val defender: Entity, val damage: Double, val event: EntityDamageByEntityEvent?, val type: DamageType, val context: ScriptContext? = null): BukkitProxyEvent() {

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
}