package org.gitee.orryx.utils

import kotlinx.coroutines.Dispatchers
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.SyncDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * Minecraft async dispatcher.
 */
val Dispatchers.async: CoroutineContext
    get() = AsyncDispatcher

/**
 * Minecraft sync dispatcher.
 */
val Dispatchers.minecraft: CoroutineContext
    get() = SyncDispatcher
