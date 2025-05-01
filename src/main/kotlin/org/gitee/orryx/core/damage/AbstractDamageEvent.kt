package org.gitee.orryx.core.damage

import com.eatthepath.uuid.FastUUID
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.attribute.Damage
import org.gitee.nodens.core.attribute.Defence
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.DamageType.*
import org.gitee.orryx.compat.nodens.NodensBridge
import org.gitee.orryx.utils.apEvent
import org.gitee.orryx.utils.isAttributePlus
import org.gitee.orryx.utils.isNodens
import org.gitee.orryx.utils.noEvent
import taboolib.module.kether.ScriptContext
import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

abstract class AbstractDamageEvent(
    val attacker: Entity,
    val defender: Entity,
    private var privateDamage: Double,
    val event: EntityDamageByEntityEvent?,
    var type: DamageType,
    val context: ScriptContext? = null
) : BukkitProxyEvent() {

    internal var origin: Any? = null

    var damage: Double
        get() = when {
            isAttributePlus() -> apEvent()!!.damage
            else -> privateDamage
        }
        set(value) {
            when {
                value > damage -> {
                    addDamage(value - damage, type)
                }
                value < damage -> {
                    takeDamage(damage - value, type)
                }
            }
        }

    fun addDamage(damage: Double, type: DamageType) {
        if (damage < 0) takeDamage(-damage, type)
        when {
            isAttributePlus() -> apEvent()!!.damage += damage
            isNodens() -> {
                when(type) {
                    PHYSICS -> noEvent()!!.processor.addDamageSource("Orryx@${FastUUID.toString(UUID.randomUUID())}", Damage.Physics, damage)
                    MAGIC -> noEvent()!!.processor.addDamageSource("Orryx@${FastUUID.toString(UUID.randomUUID())}", Damage.Magic, damage)
                    REAL -> noEvent()!!.processor.addDamageSource("Orryx@${FastUUID.toString(UUID.randomUUID())}", Damage.Real, damage)
                    else -> error("unsupported $type")
                }
            }
            else -> privateDamage += damage
        }
    }

    fun takeDamage(damage: Double, type: DamageType) {
        if (damage < 0) addDamage(-damage, type)
        when {
            isAttributePlus() -> apEvent()!!.damage -= damage
            isNodens() -> {
                when(type) {
                    PHYSICS -> noEvent()!!.processor.addDefenceSource("Orryx@${FastUUID.toString(UUID.randomUUID())}", Defence.Physics, damage)
                    MAGIC -> noEvent()!!.processor.addDamageSource("Orryx@${FastUUID.toString(UUID.randomUUID())}", Defence.Magic, damage)
                    else -> error("unsupported $type")
                }
            }
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