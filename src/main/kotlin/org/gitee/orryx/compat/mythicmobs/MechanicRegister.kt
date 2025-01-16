package org.gitee.orryx.compat.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicConditionLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicTargeterLoadEvent
import org.gitee.orryx.compat.mythicmobs.targeter.*
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent

object MechanicRegister {

    @Ghost
    @SubscribeEvent
    private fun registerMechanic(event: MythicMechanicLoadEvent) {
        when (event.mechanicName.uppercase()) {

        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerTargeter(event: MythicTargeterLoadEvent) {
        when (event.targeterName.uppercase()) {
            "O-LIC" -> { event.register(MythicMobsLivingInConeTargeter(event.config)) }
            "O-FORWARD" -> { event.register(MythicMobsForwardTargeter(event.config)) }
            "O-MIR" -> { event.register(MythicMobsMobInRangeTargeter(event.config)) }
            "O-RECTANGLE" -> { event.register(MythicMobsRectangleTargeter(event.config)) }
            "O-RANGE" -> { event.register(MythicMobsOffsetRangeTargeter(event.config)) }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerCondition(event: MythicConditionLoadEvent) {
        when (event.conditionName.uppercase()) {

        }
    }

}