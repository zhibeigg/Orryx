package org.gitee.orryx.compat.arcartx.glimmer

import org.gitee.orryx.utils.ArcartXPlugin
import priv.seventeen.artist.arcartx.glimmer.callable.CallableManager
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang

object OrryxGlimmerRegister {

    @Awake(LifeCycle.ENABLE)
    private fun register() {
        if (!ArcartXPlugin.isEnabled) return
        try {
            CallableManager.INSTANCE.registerStaticFunction(OrryxGlimmerFunctions::class.java)
            CallableManager.INSTANCE.registerGlimmerObject(OrryxPlayerObject::class.java)
            console().sendLang("hook-true", "ArcartX-Glimmer")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
