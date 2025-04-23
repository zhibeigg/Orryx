package org.gitee.orryx.compat.nodens

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.DigitalParser
import org.gitee.nodens.common.EntitySyncProfile
import org.gitee.nodens.core.AttributeConfig
import org.gitee.nodens.core.AttributeManager
import org.gitee.nodens.core.IAttributeGroup
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.attribute.Damage
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.DamageType.*
import org.gitee.orryx.compat.IAttributeBridge
import taboolib.module.kether.ScriptContext

class NodensBridge: IAttributeBridge {

    object Fire: IAttributeGroup.Number {

        override val name: String = "Fire"

        override val config: AttributeConfig
            get() = AttributeManager.getConfig(Damage.name, name)

        override fun handleAttacker(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
        }

        override fun handleDefender(damageProcessor: DamageProcessor, valueMap: Map<DigitalParser.Type, DoubleArray>) {
        }

        override fun sync(entitySyncProfile: EntitySyncProfile, valueMap: Map<DigitalParser.Type, DoubleArray>) {
        }
    }

    override fun addAttribute(entity: LivingEntity, key: String, value: List<String>, timeout: Long) {
        Nodens.api().addTempAttribute(entity, key, TempAttributeData(timeout, Nodens.api().matchAttributes(value), false))
    }

    override fun damage(
        attacker: LivingEntity,
        target: LivingEntity,
        damage: Double,
        type: DamageType,
        context: ScriptContext?
    ) {
        val processor = DamageProcessor(type.name, attacker, target)
        when (type) {
            MAGIC -> processor.addDamageSource("Orryx", Damage.Magic, damage)
            PHYSICS -> processor.addDamageSource("Orryx", Damage.Physics, damage)
            FIRE -> processor.addDamageSource("Orryx", Fire, damage)
            REAL -> processor.addDamageSource("Orryx", Damage.Real, damage)
            SELF -> processor.addDamageSource("Orryx", Damage.Real, damage)
            CONSOLE -> processor.addDamageSource("Orryx", Damage.Real, damage)
            CUSTOM -> processor.addDamageSource("Orryx", Damage.Real, damage)
        }
        Nodens.api().attackEntity(processor)
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        Nodens.api().removeTempAttribute(entity, key)
    }
}