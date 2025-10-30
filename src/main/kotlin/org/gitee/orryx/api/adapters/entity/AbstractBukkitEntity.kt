package org.gitee.orryx.api.adapters.entity

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import java.util.*

open class AbstractBukkitEntity(private val instance: Entity) : IEntity, ITargetEntity<Entity>, ITargetLocation<Entity> {

    override val entity: IEntity
        get() = this

    override val location: Location
        get() = instance.location

    override fun getSource(): Entity = instance

    override val entityId: Int
        get() = instance.entityId

    override val isDead: Boolean
        get() = instance.isDead

    override val isValid: Boolean
        get() = instance.isValid

    override val eyeLocation: Location
        get() = (instance as? LivingEntity)?.eyeLocation ?: location

    override val uniqueId: UUID
        get() = instance.uniqueId

    override val world: World
        get() = instance.world

    override val name: String
        get() = instance.name

    override val gravity: Boolean
        get() = instance.hasGravity()

    override var customName: String?
        get() = instance.customName
        set(value) { instance.customName = value }

    override val type: String
        get() = instance.type.name

    override val bukkitType: EntityType
        get() = instance.type

    override val height: Double
        get() = instance.height

    override val width: Double
        get() = instance.width

    override val vehicle: IEntity?
        get() = instance.vehicle?.let { AbstractBukkitEntity(it) }

    val isLivingEntity: Boolean
        get() = instance is LivingEntity

    override var velocity: Vector
        get() = instance.velocity
        set(value) { instance.velocity = value }

    override val moveSpeed: Double
        get() = (instance as? LivingEntity)?.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.value ?: 0.0

    override val isOnGround: Boolean
        get() = instance.isOnGround

    override val isFrozen: Boolean
        get() = instance.isFrozen

    override val isFired: Boolean
        get() = instance.isVisualFire

    override val isSilent: Boolean
        get() = instance.isSilent

    override val isCustomNameVisible: Boolean
        get() = instance.isCustomNameVisible

    override val isGlowing: Boolean
        get() = instance.isGlowing

    override val isInWater: Boolean
        get() = instance.isInWater

    override val isInvulnerable: Boolean
        get() = instance.isInvulnerable

    override val isInsideVehicle: Boolean
        get() = instance.isInsideVehicle

    override fun teleport(location: Location) {
        instance.teleport(location)
    }

    override fun remove() {
        instance.remove()
    }

    override fun hashCode(): Int = instance.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        this -> true
        is AbstractBukkitEntity -> instance == other.instance
        is Entity -> instance == other
        else -> false
    }

    override fun toString(): String {
        return "AbstractBukkitEntity(name=${instance.name}, type=${instance.type}, uuid=${instance.uniqueId})"
    }

}
