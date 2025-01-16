package org.gitee.orryx.api.adapters.entity

import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.entity.type.AdyEntityLiving
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.AbstractEntity
import org.gitee.orryx.core.targets.ITargetEntity
import java.util.*

class AbstractAdyeshachEntity(val instance: EntityInstance) : AbstractEntity, ITargetEntity<EntityInstance> {

    override fun getSource(): EntityInstance {
        return instance
    }

    override val entity: AbstractEntity
        get() = this

    override val entityId: Int
        get() = instance.index

    val id: String
        get() = instance.id

    override val uniqueId: UUID
        get() = instance.normalizeUniqueId

    override val world: World
        get() = instance.world

    override val name: String
        get() = instance.getCustomName()

    val isDeleted: Boolean
        get() = instance.isRemoved

    override val isDead: Boolean
        get() = if(instance is AdyEntityLiving) {
            instance.isDie
        } else {
            false
        }

    override val isValid: Boolean
        get() = !isDeleted

    override var customName: String?
        get() = instance.getCustomName()
        set(value) {
            instance.setCustomName(value ?: error("Not name."))
        }

    override val gravity: Boolean
        get() = !instance.isNoGravity()

    override val type: String
        get() = instance.entityType.name

    override val bukkitType: EntityType
        get() = Adyeshach.api().getEntityTypeRegistry().getBukkitEntityType(instance.entityType)

    override val height: Double
        get() = instance.entitySize.height

    override val width: Double
        get() = instance.entitySize.width

    override val vehicle: AbstractEntity?
        get() = instance.getVehicle()?.let { AbstractAdyeshachEntity(it) }

    override val location: Location
        get() = instance.getLocation()

    override val eyeLocation: Location
        get() = instance.getEyeLocation()

    override var velocity: Vector
        get() = instance.getVelocity()
        set(value) {
            val vector = Vector(value.x, value.y, value.z)
            instance.setVelocity(vector)
        }


    override fun hashCode(): Int {
        return instance.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is EntityInstance) {
            return instance == other
        }

        if (other is AbstractAdyeshachEntity) {
            return other.instance == instance
        }

        return false
    }

    override fun teleport(location: Location) {
        instance.teleport(location)
    }

    override fun remove() {
        instance.remove()
    }

    override fun toString(): String {
        return "ady{id:${instance.id},uuid:${instance.uniqueId}}"
    }


}