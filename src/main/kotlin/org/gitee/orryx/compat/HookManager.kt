package org.gitee.orryx.compat

import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang

object HookManager {

    @Awake(LifeCycle.ENABLE)
    private fun load() {
        if (AdyeshachEnabled) {
            console().sendLang("hook-true", "Adyeshach")
        } else {
            console().sendLang("hook-false", "Adyeshach")
        }
        if (DragonCoreEnabled) {
            console().sendLang("hook-true", "DragonCore")
        } else {
            console().sendLang("hook-false", "DragonCore")
        }
        if (MythicMobsEnabled) {
            console().sendLang("hook-true", "MythicMobs")
        } else {
            console().sendLang("hook-false", "MythicMobs")
        }
        if (RedisChannelEnabled) {
            console().sendLang("hook-true", "RedisChannel")
        } else {
            console().sendLang("hook-false", "RedisChannel")
        }
        if (OriginAttributeEnabled) {
            console().sendLang("hook-true", "OriginAttribute")
        } else {
            console().sendLang("hook-false", "OriginAttribute")
        }
        if (AttributePlusEnabled) {
            console().sendLang("hook-true", "AttributePlus")
        } else {
            console().sendLang("hook-false", "AttributePlus")
        }
    }

}