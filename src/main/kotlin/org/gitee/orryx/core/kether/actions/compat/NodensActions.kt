package org.gitee.orryx.core.kether.actions.compat

import org.bukkit.entity.LivingEntity
import org.gitee.nodens.common.DamageProcessor
import org.gitee.nodens.common.RegainProcessor
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.combinationParser
import org.gitee.orryx.utils.mapInstance
import org.gitee.orryx.utils.theyContainer
import taboolib.module.kether.KetherParser

object NodensActions {

    @KetherParser(["damageProcessor"], namespace = NODENS_NAMESPACE, shared = true)
    private fun damageProcessor() = combinationParser(
        Action.new("Nodens", "创建伤害处理器DamageProcessor", "damageProcessor", true)
            .description("创建伤害处理器DamageProcessor")
            .addEntry("伤害类型", Type.STRING)
            .addContainerEntry("防御者defender")
            .result("伤害处理器列表", Type.ITERABLE)
    ) {
        it.group(
            text(),
            theyContainer(false)
        ).apply(it) { type, they ->
            now {
                val attacker = bukkitPlayer()
                they!!.mapInstance<ITargetEntity<LivingEntity>, DamageProcessor> { entity ->
                    DamageProcessor(type, attacker, entity.getSource())
                }
            }
        }
    }

    @KetherParser(["regainProcessor"], namespace = NODENS_NAMESPACE, shared = true)
    private fun regainProcessor() = combinationParser(
        Action.new("Nodens", "创建治疗处理器RegainProcessor", "regainProcessor", true)
            .description("创建治疗处理器RegainProcessor")
            .addEntry("治疗原因", Type.STRING)
            .addContainerEntry("受疗者passive")
            .result("治疗处理器列表", Type.ITERABLE)
    ) {
        it.group(
            text(),
            theyContainer(false)
        ).apply(it) { reason, they ->
            now {
                val healer = bukkitPlayer()
                they!!.mapInstance<ITargetEntity<LivingEntity>, RegainProcessor> { entity ->
                    RegainProcessor(reason, healer, entity.getSource())
                }
            }
        }
    }
}