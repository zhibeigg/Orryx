package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.adapters.AbstractLocation
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.ForwardTargeter
import org.bukkit.util.Vector
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsForwardTargeter(mlc: MythicLineConfig) : ForwardTargeter(mlc) {

    private val sideOffset = mlc.getDouble(arrayOf("z", "sideOffset"), 0.0)

    override fun getLocations(data: SkillMetadata): HashSet<AbstractLocation?> {
        val am = data.caster
        val targets = hashSetOf<AbstractLocation?>()
        val location = am.entity.location.clone()
            .add(am.entity.location.direction.clone().setY(0).normalize().multiply(forward))
            .add(0.0, yoffset, 0.0)
            .add(
                BukkitAdapter.adapt(
                    BukkitAdapter.adapt(am.entity.location.direction).setY(0).normalize().crossProduct(Vector(0, 1, 0))
                        .normalize().multiply(sideOffset)
                )
            )
        targets.add(location)
        return targets
    }

}