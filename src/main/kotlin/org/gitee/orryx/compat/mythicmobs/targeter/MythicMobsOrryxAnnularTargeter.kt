package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector
import taboolib.common.platform.Ghost
import kotlin.math.abs

@Ghost
class MythicMobsOrryxAnnularTargeter(mlc: MythicLineConfig): IEntitySelector(mlc) {

    private val minRadius = mlc.getPlaceholderDouble(arrayOf("minradius", "min"), 2.0)
    private val maxRadius = mlc.getPlaceholderDouble(arrayOf("maxradius", "max"), 5.0)
    private val height = mlc.getPlaceholderDouble(arrayOf("height", "h"), 2.0)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster
        val targets = HashSet<AbstractEntity?>()
        val casterLoc = am.entity.bukkitEntity.location

        val minR = minRadius.get(data)
        val maxR = maxRadius.get(data)
        val minRSq = minR * minR
        val maxRSq = maxR * maxR
        val halfHeight = height.get(data) / 2.0

        for (p in MythicMobs.inst().entityManager.getPlayers(am.entity.world)) {
            if (p.world != am.entity.world) continue
            val loc = p.bukkitEntity.location
            val dx = loc.x - casterLoc.x
            val dy = loc.y - casterLoc.y
            val dz = loc.z - casterLoc.z
            if (abs(dy) > halfHeight) continue
            val distSq = dx * dx + dz * dz
            if (distSq < minRSq || distSq > maxRSq) continue
            targets.add(p)
        }
        return targets
    }
}
