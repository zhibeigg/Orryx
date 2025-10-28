package org.gitee.orryx.utils

import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.ConfigLoader
import taboolib.module.configuration.ConfigNodeFile
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import java.io.File


internal inline fun files(path: String, vararg defs: String, callback: (File) -> Unit) {
    val file = File(getDataFolder(), path)
    if (!file.exists()) {
        defs.forEach {
            releaseResourceFile("$path/$it", false)
        }
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

internal inline fun <T> ConfigurationSection.getMap(path: String, func: (String) -> T): Map<String, T> {
    val map = HashMap<String, T>()
    getConfigurationSection(path)?.let { section ->
        section.getKeys(false).forEach { key ->
            map[key] = func(section.getString(key)!!)
        }
    }
    return map
}

internal fun loadFromFile(name: String): Configuration {
    return if (ConfigLoader.files.containsKey(name)) {
        ConfigLoader.files[name]!!.configuration
    } else {
        val file = releaseResourceFile(name)
        val conf = Configuration.loadFromFile(file, Type.YAML, false)
        ConfigLoader.files[name] = ConfigNodeFile(conf, file)
        conf
    }
}
