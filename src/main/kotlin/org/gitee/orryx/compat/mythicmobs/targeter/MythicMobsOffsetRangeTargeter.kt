package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector
import org.bukkit.util.Vector
import taboolib.common.platform.Ghost
import kotlin.math.pow

@Ghost
class MythicMobsOffsetRangeTargeter(mlc: MythicLineConfig) : IEntitySelector(mlc) {

    private val radius = mlc.getDouble(arrayOf("range", "r"), 5.0)
    private val offsetX = mlc.getDouble(arrayOf("offsetX", "ox"), 0.0)
    private val offsetY = mlc.getDouble(arrayOf("offsetY", "oy"), 0.0)
    private val offsetZ = mlc.getDouble(arrayOf("offsetZ", "oz"), 0.0)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity> {
        val am = data.caster
        val location = am.entity.bukkitEntity.location.clone()
        val offsetX = location.clone().direction.clone().setY(0).normalize().multiply(offsetX)
        val offsetY = Vector(0.0, offsetY, 0.0)
        val offsetZ = offsetX.clone().crossProduct(Vector(0, 1, 0)).multiply(offsetZ)
        val loc = BukkitAdapter.adapt(location.add(offsetX).add(offsetY).add(offsetZ))

        val targets = HashSet<AbstractEntity>()

        MythicMobs.inst().entityManager.getLivingEntities(am.entity.world).forEach {
            if (it.world == loc.world && loc.distanceSquared(it.location) < radius.pow(2.0)) {
                targets.add(it)
            }
        }
        return targets
    }

}