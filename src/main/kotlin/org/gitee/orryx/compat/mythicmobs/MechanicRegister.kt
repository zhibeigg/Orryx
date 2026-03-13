package org.gitee.orryx.compat.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicConditionLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicTargeterLoadEvent
import org.gitee.orryx.compat.mythicmobs.condition.*
import org.gitee.orryx.compat.mythicmobs.mechanic.*
import org.gitee.orryx.compat.mythicmobs.targeter.*
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent

object MechanicRegister {

    @Ghost
    @SubscribeEvent
    private fun registerCondition(event: MythicConditionLoadEvent) {
        when (event.conditionName.uppercase()) {
            "O-FLAG" -> { event.register(MythicMobsFlagCondition(event.container.conditionArgument, event.config)) }
            "O-JOB" -> { event.register(MythicMobsJobCondition(event.container.conditionArgument, event.config)) }
            "O-LEVEL" -> { event.register(MythicMobsLevelCondition(event.container.conditionArgument, event.config)) }
            "O-MANA" -> { event.register(MythicMobsManaCondition(event.container.conditionArgument, event.config)) }
            "O-SPIRIT" -> { event.register(MythicMobsSpiritCondition(event.container.conditionArgument, event.config)) }
            "O-POINT" -> { event.register(MythicMobsPointCondition(event.container.conditionArgument, event.config)) }
            "O-EXPERIENCE" -> { event.register(MythicMobsExperienceCondition(event.container.conditionArgument, event.config)) }
            "O-SKILLLEVEL" -> { event.register(MythicMobsSkillLevelCondition(event.container.conditionArgument, event.config)) }
            "O-SUPERBODY" -> { event.register(MythicMobsSuperBodyCondition(event.container.conditionArgument, event.config)) }
            "O-INVINCIBLE" -> { event.register(MythicMobsInvincibleCondition(event.container.conditionArgument, event.config)) }
            "O-SILENCE" -> { event.register(MythicMobsSilenceCondition(event.container.conditionArgument, event.config)) }
            "O-SUPERFOOT" -> { event.register(MythicMobsSuperFootCondition(event.container.conditionArgument, event.config)) }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerMechanic(event: MythicMechanicLoadEvent) {
        when (event.mechanicName.uppercase()) {
            "O-CAST" -> { event.register(MythicMobsCastMechanic(event.container.configLine, event.config)) }
            "O-GIVEMANA" -> { event.register(MythicMobsGiveManaMechanic(event.container.configLine, event.config)) }
            "O-TAKEMANA" -> { event.register(MythicMobsTakeManaMechanic(event.container.configLine, event.config)) }
            "O-GIVESPIRIT" -> { event.register(MythicMobsGiveSpiritMechanic(event.container.configLine, event.config)) }
            "O-TAKESPIRIT" -> { event.register(MythicMobsTakeSpiritMechanic(event.container.configLine, event.config)) }
            "O-SUPERBODY" -> { event.register(MythicMobsSuperBodyMechanic(event.container.configLine, event.config)) }
            "O-INVINCIBLE" -> { event.register(MythicMobsInvincibleMechanic(event.container.configLine, event.config)) }
            "O-SILENCE" -> { event.register(MythicMobsSilenceMechanic(event.container.configLine, event.config)) }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerTargeter(event: MythicTargeterLoadEvent) {
        when (event.targeterName.uppercase()) {
            "O-SELECTORL" -> { event.register(MythicMobsSelectorLocationTargeter(event.config)) }
            "O-SELECTORE" -> { event.register(MythicMobsSelectorEntityTargeter(event.config)) }
            "O-RANGE" -> { event.register(MythicMobsOrryxRangeTargeter(event.config)) }
            "O-SECTOR" -> { event.register(MythicMobsOrryxSectorTargeter(event.config)) }
            "O-OBB" -> { event.register(MythicMobsOrryxOBBTargeter(event.config)) }
            "O-ANNULAR" -> { event.register(MythicMobsOrryxAnnularTargeter(event.config)) }
            "O-FRUSTUM" -> { event.register(MythicMobsOrryxFrustumTargeter(event.config)) }
        }
    }
}
