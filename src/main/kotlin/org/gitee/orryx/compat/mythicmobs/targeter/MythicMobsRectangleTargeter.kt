package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.LivingInConeTargeter
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import org.gitee.orryx.utils.isPointInsideCuboid

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

        if (!pitch) {
            location.pitch = 0f
        }

        val vectorX1 = location.clone().direction.normalize()
        val vectorY1 =
            if (location.yaw in -360.0..-180.0 || location.yaw in 0.0..180.0) {
                vectorX1.clone().setZ(0).crossProduct(Vector(0, 0, 1)).normalize()
            } else {
                vectorX1.clone().setZ(0).crossProduct(Vector(0, 0, -1)).normalize()
            }
        val vectorZ1 = vectorX1.clone().crossProduct(vectorY1.clone()).normalize()

        val locA = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY)).add(vectorZ1.clone().multiply(-(wide / 2)))
        val locB = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY)).add(vectorZ1.clone().multiply(wide / 2))

        val locC = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY + high)).add(vectorZ1.clone().multiply(-(wide / 2)))
        val locD = location.clone().add(vectorX1.clone().multiply(forward)).add(vectorY1.clone().multiply(offsetY + high)).add(vectorZ1.clone().multiply(wide / 2))

        val locAF = locA.clone().add(vectorX1.clone().multiply(long))
        val locBF = locB.clone().add(vectorX1.clone().multiply(long))
        val locCF = locC.clone().add(vectorX1.clone().multiply(long))
        val locDF = locD.clone().add(vectorX1.clone().multiply(long))

        val array = arrayOf(locA, locB, locC, locD, locAF, locBF, locCF, locDF)

        val entities = mutableSetOf<LivingEntity>()

        am.world.livingEntities.forEach {

            if (isPointInsideCuboid(it.location, array, it.width, it.height)) {
                entities.add(it)
            }

        }

        return entities.apply { remove(am) }.map { BukkitAdapter.adapt(it) }.toHashSet()
    }

}