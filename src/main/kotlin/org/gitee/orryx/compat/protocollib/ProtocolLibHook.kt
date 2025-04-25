package org.gitee.orryx.compat.protocollib

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import org.gitee.orryx.utils.ProtocolLibPlugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object ProtocolLibHook {

    lateinit var protocolManager: ProtocolManager

    @Awake(LifeCycle.LOAD)
    private fun load() {
        if (!ProtocolLibPlugin.isEnabled) return
        protocolManager = ProtocolLibrary.getProtocolManager()
    }
}