package org.gitee.orryx.compat.nodens

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.api.Nodens
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.core.TempAttributeData
import org.gitee.nodens.core.attribute.Damage
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.DamageType.*
import org.gitee.orryx.compat.IAttributeBridge
import taboolib.module.kether.ScriptContext
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

class NodensBridge: IAttributeBridge {

    companion object {

        private val damageContexts = Collections.synchronizedMap(WeakHashMap<DamageProcessor, WeakReference<ScriptContext>>())

        fun bindContext(processor: DamageProcessor, context: ScriptContext?) {
            if (context != null) {
                damageContexts[processor] = WeakReference(context)
            }
        }

        fun contextOf(processor: DamageProcessor): ScriptContext? {
            return damageContexts[processor]?.get()
        }

        fun removeContext(processor: DamageProcessor) {
            damageContexts.remove(processor)
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
            FIRE -> processor.addDamageSource("Orryx", Damage.Magic, damage)
            REAL -> processor.addDamageSource("Orryx", Damage.Real, damage)
            MONSTER -> processor.addDamageSource("Orryx", Damage.MonsterAttack, damage)
            SELF -> processor.addDamageSource("Orryx", Damage.Real, damage)
            CONSOLE -> processor.addDamageSource("Orryx", Damage.Real, damage)
            CUSTOM -> processor.addDamageSource("Orryx", Damage.Real, damage)
        }
        bindContext(processor, context)
        try {
            processor.handleDefender()
            processor.callDamage()
        } finally {
            removeContext(processor)
        }
    }

    override fun removeAttribute(entity: LivingEntity, key: String) {
        Nodens.api().removeTempAttribute(entity, key)
    }

    override fun update(entity: LivingEntity) {
        Nodens.api().updateAttribute(entity)
    }
}