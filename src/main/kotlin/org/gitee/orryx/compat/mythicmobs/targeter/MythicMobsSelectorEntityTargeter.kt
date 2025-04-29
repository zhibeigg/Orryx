package org.gitee.orryx.compat.mythicmobs.targeter

import com.eatthepath.uuid.FastUUID
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector
import org.bukkit.entity.Entity
import org.gitee.orryx.core.kether.parameter.MythicMobsParameter
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.utils.PARAMETER
import org.gitee.orryx.utils.forEachInstance
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.adaptPlayer
import taboolib.library.kether.BlockReader
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptService
import java.util.*

@Ghost
class MythicMobsSelectorEntityTargeter(mlc: MythicLineConfig): IEntitySelector(mlc) {

    private val parse = mlc.getPlaceholderString(arrayOf("parse", "p"), "@self")

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster.entity ?: return hashSetOf()
        val caster = BukkitAdapter.adapt(am)
        val targets = hashSetOf<AbstractEntity?>()

        val context = ScriptContext.create(BlockReader(null, ScriptService, orryxEnvironmentNamespaces).parse(FastUUID.toString(UUID.randomUUID()))).also {
            it.sender = adaptPlayer(data.caster.entity.bukkitEntity)
            it[PARAMETER] = MythicMobsParameter(caster, BukkitAdapter.adapt(data.origin).toTarget())
        }
        StringParser(parse.get()).container(context).forEachInstance<ITargetEntity<Entity>> {
            targets.add(BukkitAdapter.adapt(it.getSource()))
        }
        return targets
    }
}