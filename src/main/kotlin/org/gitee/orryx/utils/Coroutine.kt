package org.gitee.orryx.utils

import kotlinx.coroutines.Dispatchers
import taboolib.expansion.AsyncDispatcher
import taboolib.expansion.SyncDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * Minecraft async dispatcher.
 */
val Dispatchers.minecraftAsync: CoroutineContext
    get() = AsyncDispatcher

/**
 * Minecraft sync dispatcher.
 */
val Dispatchers.minecraftMain: CoroutineContext
    get() = SyncDispatcher
