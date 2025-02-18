package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.LivingInConeTargeter
import org.gitee.orryx.utils.isFace
import taboolib.common.platform.Ghost
import java.util.function.Consumer
import kotlin.math.cos
import kotlin.math.pow

@Ghost
class MythicMobsLivingInConeTargeter(mlc: MythicLineConfig) : LivingInConeTargeter(mlc) {

    private val faceMe = mlc.getBoolean("face", false)
    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster
        val possible = HashSet<AbstractEntity?>()
        val var4: Iterator<*> = MythicMobs.inst().entityManager.getLivingEntities(am.entity.world).iterator()
        while (var4.hasNext()) {
            val p = var4.next() as AbstractEntity
            if (am.location.world == p.world && am.entity.location.distanceSquared(p.location) < this.range.pow(2.0)) {
                possible.add(p)
            }
        }
        val targets = HashSet<AbstractEntity?>()
        val dir = data.caster.location.direction
        if (this.rotation > 0.0) {
            dir.rotate(this.rotation.toFloat())
        }
        dir.setY(0)
        val cos = cos(this.angle * Math.PI / 180.0)
        val cosSq = cos * cos
        possible.forEach(Consumer<AbstractEntity?> { entity: AbstractEntity? ->
            val relative =
                entity!!.location.subtract(data.caster.location).toVector()
            relative.setY(0)
            val dot =
                relative.x * dir.x + relative.y * dir.y + relative.z * dir.z
            val value = dot * dot / relative.lengthSquared()
            if (this.angle < 180.0 && dot > 0.0 && value >= cosSq) {
                if (faceMe) {
                    if (entity.location.isFace(am.location)) {
                        targets.add(entity)
                    }
                } else {
                    targets.add(entity)
                }
            } else if (this.angle >= 180.0 && (dot > 0.0 || dot <= cosSq)) {
                if (faceMe) {
                    if (entity.location.isFace(am.location)) {
                        targets.add(entity)
                    }
                } else {
                    targets.add(entity)
                }
            }
        })
        return targets.apply { remove(am.entity) }
    }

}