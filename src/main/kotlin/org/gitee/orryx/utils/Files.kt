package org.gitee.orryx.utils

import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.library.configuration.ConfigurationSection
import java.io.File


internal fun files(path: String, vararg defs: String, callback: (File) -> Unit) {
    val file = File(getDataFolder(), path)
    if (!file.exists()) {
        defs.forEach { releaseResourceFile("$path/$it") }
    }
    getFiles(file).forEach { callback(it) }
}

internal fun getFiles(file: File): List<File> {
    val listOf = mutableListOf<File>()
    when (file.isDirectory) {
        true -> listOf += file.listFiles()!!.flatMap { getFiles(it) }
        false -> {
            if (file.name.endsWith(".yml")) {
                listOf += file
            }
        }
    }
    return listOf
}

internal fun ConfigurationSection.getMap(path: String): Map<String, String> {
    val map = HashMap<String, String>()
    getConfigurationSection(path)?.let { section ->
        section.getKeys(false).forEach { key ->
            map[key] = section.getString(key).toString()
        }
    }
    return map
}
