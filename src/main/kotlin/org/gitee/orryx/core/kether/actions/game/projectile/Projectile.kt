package org.gitee.orryx.core.kether.actions.game.projectile

import org.bukkit.Location
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.collider.local.ILocalCollider
import taboolib.library.kether.ParsedAction

class Projectile(
    val type: ProjectileType,
    val period: Long,
    val timeout: Long,
    val spawnLocation: Location,
    val hitbox: ILocalCollider,
    val vector: ParsedAction<*>,
    val onHit: ParsedAction<*>?,
    val onPeriod: ParsedAction<*>?
) {

    val source: IEntity? = null

    fun upDateHitBox() {
        hitbox
    }

    fun nextTick() {

    }

    fun remove() {
        if (source?.isValid == true) {
            source.remove()
        }
    }

    enum class ProjectileType {
        BUKKIT_ENTITY, ADY_ENTITY, BUKKIT_PROJECTILE, NONE;

        companion object {

            fun parseIgnoreCase(type: String): ProjectileType {
                return when (type.uppercase()) {
                    "ENTITY" -> BUKKIT_ENTITY
                    "ADY", "ADYESHACH" -> ADY_ENTITY
                    "BUKKIT" -> BUKKIT_PROJECTILE
                    "NONE" -> NONE
                    else -> error("Unknown projectile type: $type")
                }
            }
        }
    }
}