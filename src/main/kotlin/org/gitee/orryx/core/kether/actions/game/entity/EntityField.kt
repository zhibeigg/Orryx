package org.gitee.orryx.core.kether.actions.game.entity

import org.bukkit.attribute.Attribute
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.utils.abstract
import org.gitee.orryx.utils.getBukkitLivingEntity
import java.util.*

enum class EntityField(val get: IEntity.() -> Any?) {

    UUID({ uniqueId }),

    ID({ (this as? AbstractAdyeshachEntity)?.id ?: "none" }),

    NAME({ name }),

    TYPE({ type }),

    YAW({ location.yaw }),

    PITCH({ location.pitch }),

    HEIGHT({ height }),

    WIDTH({ width }),

    LOCATION({ LocationTarget(location) }),

    EYE_LOCATION({ LocationTarget(eyeLocation) }),

    DIRECTION({ location.direction.abstract() }),

    MOVE_SPEED({ moveSpeed }),

    HEALTH({ getBukkitLivingEntity()?.health }),

    VEHICLE({ vehicle }),

    VELOCITY({ velocity.abstract() }),

    BODY_IN_ARROW({ getBukkitLivingEntity()?.arrowsInBody }),

    MAX_HEALTH({ getBukkitLivingEntity()?.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 0.0 }),

    GRAVITY({ gravity }),

    FIRED({ isFired }),

    FROZEN({ isFrozen }),

    ON_GROUND({ isOnGround }),

    INSIDE_VEHICLE({ isInsideVehicle }),

    SILENT({ isSilent }),

    CUSTOM_NAME_VISIBLE({ isCustomNameVisible }),

    GLOWING({ isGlowing }),

    IN_WATER({ isInWater }),

    INVULNERABLE({ isInvulnerable }),

    DEATH({ isDead }),

    VALID({ isValid });

}