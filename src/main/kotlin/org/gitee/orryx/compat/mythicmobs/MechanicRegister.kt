package org.gitee.orryx.compat.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicTargeterLoadEvent
import org.gitee.orryx.compat.mythicmobs.mechanic.MythicMobsCastMechanic
import org.gitee.orryx.compat.mythicmobs.targeter.MythicMobsSelectorEntityTargeter
import org.gitee.orryx.compat.mythicmobs.targeter.MythicMobsSelectorLocationTargeter
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent

object MechanicRegister {

    @Ghost
    @SubscribeEvent
    private fun registerMechanic(event: MythicMechanicLoadEvent) {
        when (event.mechanicName.uppercase()) {
            "O-CAST" -> { event.register(MythicMobsCastMechanic(event.container.configLine, event.config)) }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun registerTargeter(event: MythicTargeterLoadEvent) {
        when (event.targeterName.uppercase()) {
            "O-SELECTORL" -> { event.register(MythicMobsSelectorLocationTargeter(event.config)) }
            "O-SELECTORE" -> { event.register(MythicMobsSelectorEntityTargeter(event.config)) }
        }
    }
}