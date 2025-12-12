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

    private val radius = mlc.getPlaceholderDouble(arrayOf("radius", "r"), 1.0)
    private val offsetX = mlc.getPlaceholderDouble(arrayOf("offsetx", "ox"), 0.0)
    private val offsetY = mlc.getPlaceholderDouble(arrayOf("offsety", "oy"), 0.0)
    private val offsetZ = mlc.getPlaceholderDouble(arrayOf("offsetz", "oz"), 0.0)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster
        val targets = HashSet<AbstractEntity?>()

        val vector = am.entity.bukkitEntity.location.direction.direction(offsetX.get(data), offsetY.get(data), offsetZ.get(data), false)
        val newCenter = am.entity.bukkitEntity.location.clone().add(vector)

        val radius = radius.get(data)
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