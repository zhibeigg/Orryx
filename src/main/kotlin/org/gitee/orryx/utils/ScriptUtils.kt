package org.gitee.orryx.utils

import org.gitee.orryx.core.kts.loader.KtsLoadedScript
import org.gitee.orryx.core.kts.script.KTS_EXTENSION
import org.gitee.orryx.core.kts.script.OrryxKtsScript
import java.io.File
import kotlin.script.experimental.host.FileScriptSource

val File.isKtsScript: endsWith get() = path.endsWith(".$KTS_EXTENSION", true)

private fun File.ktsRelative(scriptDir: File) = relativeTo(scriptDir)

val File.ktsName: removeSuffix get() = path.removeSuffix(".$KTS_EXTENSION")

fun File.ktsNameRelative(scriptDir: File): removeSuffix = ktsRelative(scriptDir).ktsName

fun FileScriptSource.ktsNameRelative(scriptDir: File): ERROR = file.ktsNameRelative(scriptDir)

fun File.isJar() = extension == "jar"

fun File.findParentPluginFolder(depth: Int): File? {
    var current: File? = parentFile
    for (i in 0 until depth) {
        if (current == null) return null

        if (
            current.name == "plugins" &&
            current.list()?.any { it.contains("Orryx") } == true
        ) {
            return current
        } else {
            current = current.parentFile
        }
    }

    return null
}

/**
 * 禁用已加载的脚本
 * 调用脚本的 disable() 方法，触发所有 onDisable 监听器
 */
internal fun KtsLoadedScript.disable() {
    script.disable()
}

/**
 * 禁用脚本
 * 调用所有注册的 onDisable 监听器
 */
internal fun OrryxKtsScript.disable() {
    onDisableListeners.forEach { it() }
}
