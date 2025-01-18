package org.gitee.orryx.api.events.damage

import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.orryx.core.damage.AbstractDamageEvent
import taboolib.platform.type.BukkitProxyEvent

class OrryxDamageEvents {

    class Pre(attacker: Entity, victim: Entity, privateDamage: Double, event: EntityDamageByEntityEvent?, type: DamageType): AbstractDamageEvent(attacker, victim, privateDamage, event, type)

    class Post(val attacker: Entity, val victim: Entity, val damage: Double, val event: EntityDamageByEntityEvent?, val type: DamageType): BukkitProxyEvent()

}