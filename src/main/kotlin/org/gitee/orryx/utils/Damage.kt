package org.gitee.orryx.utils

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityDamageEvent.DamageCause.*
import org.gitee.nodens.api.events.entity.NodensEntityDamageEvents
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.core.damage.AbstractDamageEvent
import org.serverct.ersha.api.event.AttrEntityDamageBeforeEvent
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty

fun AbstractDamageEvent.isAttributePlus(): Boolean {
    return try {
        origin is AttrEntityDamageBeforeEvent
    } catch (_: Throwable) {
        false
    }
}

fun AbstractDamageEvent.isNodens(): Boolean {
    return try {
        origin is NodensEntityDamageEvents.Pre
    } catch (_: Throwable) {
        false
    }
}

fun AbstractDamageEvent.apEvent(): AttrEntityDamageBeforeEvent? {
    return try {
        origin as? AttrEntityDamageBeforeEvent
    } catch (_: Throwable) {
        null
    }
}

fun AbstractDamageEvent.noEvent(): NodensEntityDamageEvents.Pre? {
    return try {
        origin as? NodensEntityDamageEvents.Pre
    } catch (_: Throwable) {
        null
    }
}

fun doDamage(source: LivingEntity?, entity: LivingEntity, damageCause: DamageCause, damage: Double) {
    // 如果实体血量 - 预计伤害值 < 0 提前设置击杀者
    if (entity.health - damage <= 0 && source is Player) {
        entity.setProperty("entity/killer", source.getProperty("entity"))
    }
    entity.noDamageTicks = 0
    if (source != null) {
        entity.lastDamageCause = EntityDamageByEntityEvent(source, entity, damageCause, damage)
    }
    entity.damage(damage)
}

fun transfer(damageCause: DamageCause): DamageType {
    return try {
        when (damageCause) {
            CUSTOM -> DamageType.MAGIC
            FALL -> DamageType.PHYSICS
            ENTITY_ATTACK -> DamageType.PHYSICS
            DRAGON_BREATH -> DamageType.MAGIC
            MAGIC -> DamageType.MAGIC
            ENTITY_EXPLOSION -> DamageType.MAGIC
            BLOCK_EXPLOSION -> DamageType.MAGIC
            PROJECTILE -> DamageType.PHYSICS
            FIRE -> DamageType.FIRE
            DRYOUT -> DamageType.SELF
            DROWNING -> DamageType.SELF
            SUICIDE -> DamageType.CONSOLE
            LAVA -> DamageType.FIRE
            POISON -> DamageType.MAGIC
            VOID -> DamageType.MAGIC
            LIGHTNING -> DamageType.MAGIC
            HOT_FLOOR -> DamageType.FIRE
            FREEZE -> DamageType.MAGIC
            CRAMMING -> DamageType.PHYSICS
            THORNS -> DamageType.PHYSICS
            CONTACT -> DamageType.MAGIC
            ENTITY_SWEEP_ATTACK -> DamageType.PHYSICS
            SUFFOCATION -> DamageType.SELF
            FIRE_TICK -> DamageType.FIRE
            MELTING -> DamageType.PHYSICS
            STARVATION -> DamageType.SELF
            WITHER -> DamageType.MAGIC
            FALLING_BLOCK -> DamageType.PHYSICS
            FLY_INTO_WALL -> DamageType.PHYSICS
            KILL -> DamageType.CONSOLE
            WORLD_BORDER -> DamageType.CONSOLE
            SONIC_BOOM -> DamageType.MAGIC
        }
    } catch (_: Throwable) {
        DamageType.CONSOLE
    }
}