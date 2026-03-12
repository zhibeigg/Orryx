package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector
import taboolib.common.platform.Ghost
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Ghost
class MythicMobsOrryxSectorTargeter(mlc: MythicLineConfig): IEntitySelector(mlc) {

    private val radius = mlc.getPlaceholderDouble(arrayOf("radius", "r"), 5.0)
    private val angle = mlc.getPlaceholderDouble(arrayOf("angle", "a"), 90.0)
    private val height = mlc.getPlaceholderDouble(arrayOf("height", "h"), 2.0)
    private val offsetY = mlc.getPlaceholderDouble(arrayOf("offsety", "oy"), 0.0)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster
        val targets = HashSet<AbstractEntity?>()
        val casterLoc = am.entity.bukkitEntity.location
        val radius = radius.get(data)
        val radiusSq = radius * radius
        val halfAngle = angle.get(data) / 2.0
        val halfHeight = height.get(data) / 2.0
        val oy = offsetY.get(data)

        val yaw = Math.toRadians(-casterLoc.yaw.toDouble())
        val dirX = sin(yaw)
        val dirZ = cos(yaw)

        for (p in MythicMobs.inst().entityManager.getPlayers(am.entity.world)) {
            if (p.world != am.entity.world) continue
            val loc = p.bukkitEntity.location
            val dx = loc.x - casterLoc.x
            val dy = loc.y - (casterLoc.y + oy)
            val dz = loc.z - casterLoc.z
            if (abs(dy) > halfHeight) continue
            val distSq = dx * dx + dz * dz
            if (distSq > radiusSq) continue
            if (distSq > 0.001) {
                val angleBetween = Math.toDegrees(atan2(dirX * dz - dirZ * dx, dirX * dx + dirZ * dz))
                if (abs(angleBetween) > halfAngle) continue
            }
            targets.add(p)
        }
        return targets
    }
}
