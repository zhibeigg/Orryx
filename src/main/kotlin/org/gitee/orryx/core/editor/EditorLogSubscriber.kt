package org.gitee.orryx.core.editor

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
 * 日志订阅器
 * 拦截服务器日志 → 通过 EditorClient 推送 log.entry 到中心服务器
 */
@PlatformSide(Platform.BUKKIT)
object EditorLogSubscriber {

    private var handler: Handler? = null

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        install()
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        uninstall()
    }

    private fun install() {
        if (handler != null) return
        val logHandler = object : Handler() {
            override fun publish(record: LogRecord) {
                if (!EditorClient.isConnected()) return
                val level = when (record.level) {
                    Level.SEVERE -> "ERROR"
                    Level.WARNING -> "WARN"
                    Level.INFO -> "INFO"
                    else -> "DEBUG"
                }
                EditorClient.pushLog(level, record.message ?: "", record.loggerName)
            }

            override fun flush() {}
            override fun close() {}
        }
        handler = logHandler
        Logger.getLogger("").addHandler(logHandler)
    }

    private fun uninstall() {
        handler?.let {
            Logger.getLogger("").removeHandler(it)
            handler = null
        }
    }
}
