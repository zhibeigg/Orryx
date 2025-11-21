package org.gitee.orryx.core.kts

import java.io.File

/** 服务器不支持的错误消息 */
const val SERVER_NOT_SUPPORTED_MESSAGE = "Kts 不支持 CraftBukkit RAW 服务器，请尝试 Spigot 或其任何分支，例如 Paper"

/**
 * Kts 异常基类
 *
 * 所有 Kts 相关的异常都继承此类
 */
abstract class KtsException(message: String) : Exception(message)

/**
 * 服务器不支持异常
 * 当在 CraftBukkit 服务器上运行时抛出
 */
class ServerNotSupportedException : KtsException(SERVER_NOT_SUPPORTED_MESSAGE)

/**
 * 脚本未找到异常
 *
 * @property scriptName 脚本名称
 */
class ScriptNotFoundException(message: String, val scriptName: String) : KtsException(message)

/**
 * 脚本状态无效异常
 * 当脚本处于不允许的状态时抛出（例如尝试加载已加载的脚本）
 *
 * @property state 当前状态名称
 */
class ScriptInvalidStateException(message: String, val state: String) : KtsException(message)

/**
 * 检索脚本定义异常
 * 当无法从脚本文件中提取元数据时抛出
 *
 * @property scriptName 脚本名称
 */
class RetrieveScriptDefinitionException(message: String, val scriptName: String) : KtsException(message)

/**
 * 脚本文件不存在异常
 *
 * @property scriptName 脚本名称
 * @property file 文件对象
 */
class ScriptFileDoesNotExistException(message: String, val scriptName: String, val file: File) : KtsException(message)

/**
 * 编译异常
 * 当脚本编译失败时抛出，包装底层编译器异常
 *
 * @param exception 原始异常
 */
class KtsCompilationException(exception: Throwable) : Exception(exception)

/**
 * 加载异常
 * 当脚本加载失败时抛出，包装底层加载器异常
 *
 * @param exception 原始异常
 */
class KtsLoadException(exception: Throwable) : Throwable(exception)