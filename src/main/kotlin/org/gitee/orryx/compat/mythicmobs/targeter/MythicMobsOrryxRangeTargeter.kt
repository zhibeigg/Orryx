package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector
import org.gitee.orryx.utils.direction
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsOrryxRangeTargeter(mlc: MythicLineConfig): IEntitySelector(mlc) {

    private val radius = mlc.getDouble(arrayOf("radius", "r"), 1.0)
    private val offsetX = mlc.getDouble(arrayOf("offsetx", "ox"), 0.0)
    private val offsetY = mlc.getDouble(arrayOf("offsety", "oy"), 0.0)
    private val offsetZ = mlc.getDouble(arrayOf("offsetz", "oz"), 0.0)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster
        val targets = HashSet<AbstractEntity?>()

        val vector = am.entity.bukkitEntity.location.direction.direction(offsetX, offsetY, offsetZ, false)
        val newCenter = am.entity.bukkitEntity.location.clone().add(vector)

        val radiusSq: Double = radius * radius
        for (p in MythicMobs.inst().entityManager.getPlayers(am.entity.world)) {
            if (p.world != am.entity.world) {
                continue
            }
            if (newCenter.distanceSquared(p.bukkitEntity.location) >= radiusSq) {
                continue
            }
            targets.add(p)
        }
        return targets
    }
}