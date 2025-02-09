package org.gitee.orryx.api.adapters

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import java.util.*

interface IEntity {

    val uniqueId: UUID

    val isDead: Boolean

    val isValid: Boolean

    val world: World

    val type: String

    val bukkitType: EntityType?

    val width: Double

    val height: Double

    val vehicle: IEntity?

    val name: String

    var customName: String?

    val location: Location

    val eyeLocation: Location

    var velocity: Vector

    val entityId: Int

    val gravity: Boolean

    fun teleport(location: Location)

    fun remove()

}