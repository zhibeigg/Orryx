package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.LivingInConeTargeter
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.utils.AABB
import org.gitee.orryx.utils.areAABBsColliding
import org.gitee.orryx.utils.getEntityAABB
import org.gitee.orryx.utils.joml
import org.joml.Vector3d
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsRectangleTargeter(mlc: MythicLineConfig) : LivingInConeTargeter(mlc) {

    private val long = mlc.getDouble(arrayOf("long", "l"), 0.0)
    private val wide = mlc.getDouble(arrayOf("wide", "w"), 0.0)
    private val high = mlc.getDouble(arrayOf("high", "h"), 0.0)
    private val forward = mlc.getDouble(arrayOf("forward", "f"), 0.0)
    private val offsetY = mlc.getDouble(arrayOf("offsetY", "o"), 0.0)
    private val pitch = mlc.getBoolean(arrayOf("pitch", "p"), false)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity> {
        val am = data.caster.entity.bukkitEntity
        val location = am.location.clone()

        if (!pitch) { location.pitch = 0f }

        val vectorX = location.direction.clone().setY(0).joml().normalize()
        val vectorZ = vectorX.cross(Vector3d(0.0, 1.0, 0.0), Vector3d()).normalize()
        val vectorY = location.direction.clone().joml().cross(vectorZ, Vector3d()).normalize()
        val minVector = AbstractVector(location.toVector()).add(vectorX.mul(-long/2+forward, Vector3d())).add(vectorY.mul(-high/2+offsetY)).add(vectorZ.mul(-wide/2, Vector3d()))
        val maxVector = AbstractVector(location.toVector()).add(vectorX.mul(long/2+forward, Vector3d())).add(vectorY.mul(high/2+offsetY)).add(vectorZ.mul(wide/2, Vector3d()))

        val box = AABB(minVector, maxVector)

        val entities = am.world.livingEntities.filter {
            it != am && areAABBsColliding(box, getEntityAABB(it))
        }

        return entities.map { BukkitAdapter.adapt(it) }.toHashSet()
    }

}