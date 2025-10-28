package org.gitee.orryx.compat.mythicmobs.targeter

import com.eatthepath.uuid.FastUUID
import io.lumine.xikage.mythicmobs.adapters.AbstractLocation
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.ILocationSelector
import org.bukkit.Location
import org.gitee.orryx.core.kether.parameter.MythicMobsParameter
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.PARAMETER
import org.gitee.orryx.utils.forEachInstance
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.adaptPlayer
import taboolib.library.kether.BlockReader
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptService
import taboolib.module.kether.runKether
import java.util.*

@Ghost
class MythicMobsSelectorLocationTargeter(mlc: MythicLineConfig): ILocationSelector(mlc) {

    private val parse = mlc.getPlaceholderString(arrayOf("parse", "p"), "@self")

    override fun getLocations(data: SkillMetadata): HashSet<AbstractLocation?> {
        return runKether(hashSetOf()) {
            val am = data.caster.entity ?: return@runKether hashSetOf()
            val caster = BukkitAdapter.adapt(am)
            val targets = hashSetOf<AbstractLocation?>()

            val context = ScriptContext.create(BlockReader(null, ScriptService, orryxEnvironmentNamespaces).parse(FastUUID.toString(UUID.randomUUID()))).also {
                it.sender = adaptPlayer(data.caster.entity.bukkitEntity)
                it[PARAMETER] = MythicMobsParameter(caster, BukkitAdapter.adapt(data.origin).toTarget())
            }
            StringParser(parse.get()).syncContainer(context).forEachInstance<ITargetLocation<Location>> {
                targets.add(BukkitAdapter.adapt(it.getSource()))
            }
            return@runKether targets
        }!!
    }
}