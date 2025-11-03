package org.gitee.orryx.api.adapters.entity

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.core.targets.ITargetLocation
import java.util.*

open class AbstractNoneEntity(private var privateLocation: Location) : IEntity, ITargetLocation<AbstractNoneEntity> {

    override var customName: String? = null

    override val entityId: Int
        get() = -1

    override val eyeLocation: Location
        get() = location

    override val location: Location
        get() = privateLocation

    override val gravity: Boolean
        get() = false

    override val width: Double
        get() = 0.0

    override val height: Double
        get() = 0.0

    override val isDead: Boolean
        get() = false

    override val isFired: Boolean
        get() = false

    override val isFrozen: Boolean
        get() = false

    override val isCustomNameVisible: Boolean
        get() = false

    override val isGlowing: Boolean
        get() = false

    override val isInWater: Boolean
        get() = false

    override val isSilent: Boolean
        get() = true

    override val isInsideVehicle: Boolean
        get() = false

    override val isInvulnerable: Boolean
        get() = true

    override val vehicle: IEntity?
        get() = null

    override val isOnGround: Boolean
        get() = false

    override val isValid: Boolean
        get() = true

    override val moveSpeed: Double
        get() = 0.0

    override val uniqueId: UUID = UUID.randomUUID()

    override val bukkitType: EntityType?
        get() = null

    override val world: World
        get() = location.world!!

    override var velocity: Vector = Vector()

    override val type: String
        get() = ""

    override val name: String
        get() = ""

    override fun getSource(): AbstractNoneEntity {
        return this
    }

    override fun remove() {

    }

    override fun teleport(location: Location) {
        privateLocation = location
    }
}