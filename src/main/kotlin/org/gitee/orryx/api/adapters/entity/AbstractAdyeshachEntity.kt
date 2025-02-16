package org.gitee.orryx.api.adapters.entity

import ink.ptms.adyeshach.core.Adyeshach
import ink.ptms.adyeshach.core.entity.EntityInstance
import ink.ptms.adyeshach.core.entity.type.AdyEntityLiving
import ink.ptms.adyeshach.impl.entity.trait.impl.isTraitSit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import taboolib.common5.cdouble
import java.util.*

open class AbstractAdyeshachEntity(private val instance: EntityInstance) : IEntity, ITargetEntity<EntityInstance>, ITargetLocation<EntityInstance> {

    override val entity: IEntity
        get() = object : IEntity by this {}

    override val location: Location
        get() = instance.getLocation().clone()

    override val eyeLocation: Location
        get() = instance.getEyeLocation().clone()

    override fun getSource(): EntityInstance = instance

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
        get() = (instance as? AdyEntityLiving)?.isDie ?: false

    override val isValid: Boolean
        get() = !isDeleted

    override var customName: String?
        get() = instance.getCustomName()
        set(value) {
            requireNotNull(value) { "Custom name cannot be null" }
            instance.setCustomName(value)
        }

    override var velocity: Vector
        get() = instance.getVelocity().clone()
        set(value) {
            instance.setVelocity(value.clone())
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

    override val vehicle: IEntity?
        get() = instance.getVehicle()?.let { AbstractAdyeshachEntity(it) }

    override val moveSpeed: Double
        get() = instance.moveSpeed

    override val isOnGround: Boolean
        get() = instance.getLocation().let { loc ->
            loc.y == loc.blockY.cdouble
        }

    override val isFrozen: Boolean
        get() = false

    override val isFired: Boolean
        get() = instance.isFired()

    override val isCustomNameVisible: Boolean
        get() = instance.isCustomNameVisible()

    override val isGlowing: Boolean
        get() = instance.isGlowing()

    override val isInWater: Boolean
        get() = eyeLocation.block.type == Material.WATER

    override val isInsideVehicle: Boolean
        get() = instance.isTraitSit()

    override val isInvulnerable: Boolean
        get() = true

    override val isSilent: Boolean
        get() = instance.isNitwit

    override fun teleport(location: Location) {
        instance.teleport(location.clone())
    }

    override fun remove() {
        instance.remove()
    }

    override fun hashCode(): Int = instance.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        this -> true
        is EntityInstance -> instance == other
        is AbstractAdyeshachEntity -> instance == other.instance
        else -> false
    }

    override fun toString(): String {
        return "AbstractAdyeshachEntity(id=${instance.id}, type=${type}, world=${world.name})"
    }

}
