package org.gitee.orryx.module.state

import com.germ.germplugin.api.event.GermClientLinkedEvent
import com.germ.germplugin.api.event.GermKeyDownEvent
import com.germ.germplugin.api.event.GermKeyUpEvent
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import eos.moe.dragoncore.api.event.EntityJoinWorldEvent
import eos.moe.dragoncore.api.event.EntityLeaveWorldEvent
import eos.moe.dragoncore.api.event.KeyPressEvent
import eos.moe.dragoncore.api.event.KeyReleaseEvent
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.module.state.states.*
import org.gitee.orryx.utils.*
import priv.seventeen.artist.arcartx.event.client.ClientChannelEvent
import priv.seventeen.artist.arcartx.event.client.ClientEntityJoinEvent
import priv.seventeen.artist.arcartx.event.client.ClientEntityLeaveEvent
import priv.seventeen.artist.arcartx.event.client.ClientKeyPressEvent
import priv.seventeen.artist.arcartx.event.client.ClientKeyReleaseEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import taboolib.common5.cbool
import taboolib.common5.cfloat
import taboolib.common5.clong
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptService
import taboolib.module.kether.orNull
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object StateManager {

    private val playerDataMap = ConcurrentHashMap<UUID, PlayerData>()
    private val statusMap = ConcurrentHashMap<String, Status>()
    private val controllerMap = ConcurrentHashMap<String, Configuration>()
    private val globalState = ConcurrentHashMap<String, IActionState>()

    private val playerInvisibleHandTaskMap = ConcurrentHashMap<UUID, SimpleTimeoutTask>()

    // 按键事件节流 Map，key 为 "玩家UUID_按键"，value 为上次处理时间
    private val keyPressThrottleMap = ConcurrentHashMap<String, Long>()
    private const val THROTTLE_INTERVAL = 50L // 50ms 节流间隔

    @Config("state.yml")
    lateinit var state: ConfigFile
        private set

    /**
     * @see org.gitee.orryx.core.skill.SkillLoaderManager.reload
     * */
    internal fun reload(skillMap: Map<String, ICastSkill>) {
        globalState.clear()
        state.reload()
        state.getConfigurationSection("GlobalStates")?.getKeys(false)?.forEach {
            globalState[it] = load(it, state.getConfigurationSection("GlobalStates.$it")!!)
        }
        skillMap.forEach { (t, u) ->
            globalState[t] = SkillState(u)
        }
        consoleMessage("&e┣&7GlobalState loaded &e${globalState.size} &a√")
    }

    @Awake(LifeCycle.ENABLE)
    @Reload(2)
    private fun reload() {
        controllerMap.clear()
        files("controllers", "example.yml") { file ->
            controllerMap[file.nameWithoutExtension] = Configuration.loadFromFile(file)
        }
        consoleMessage("&e┣&7Controllers loaded &e${controllerMap.size} &a√")
        statusMap.clear()
        files("status", "example.yml") { file ->
            statusMap[file.nameWithoutExtension] = Status(file.nameWithoutExtension, Configuration.loadFromFile(file))
        }
        consoleMessage("&e┣&7Status loaded &e${statusMap.size} &a√")
        onlinePlayers.forEach {
            autoCheckStatus(it)
        }
    }

    fun load(key: String, configurationSection: ConfigurationSection): IActionState {
        return when (val type = configurationSection.getString("Type")?.uppercase()) {
            BLOCK_STATE -> BlockState(key, configurationSection)
            DODGE_STATE -> DodgeState(key, configurationSection)
            GENERAL_ATTACK_STATE -> GeneralAttackState(key, configurationSection)
            PRESS_ATTACK_STATE -> PressGeneralAttackState(key, configurationSection)
            VERTIGO_STATE -> VertigoState(key, configurationSection)
            else -> error("state not support $type")
        }
    }

    @Ghost
    @SubscribeEvent
    private fun join(e: EntityJoinWorldEvent) {
        val joiner = Bukkit.getPlayer(e.entityUUID) ?: return
        val joinerData = joiner.statusData()
        val data = e.player.statusData()
        data.cacheJoiner.add(joiner.uniqueId)
        val joinerStatus = joinerData.status as? Status ?: return
        val controller = controllerMap[joinerStatus.options.controller] ?: return
        DragonCoreCustomPacketSender.setPlayerAnimationController(e.player, joiner.uniqueId, controller.saveToString())
    }

    @Ghost
    @SubscribeEvent
    private fun leave(e: EntityLeaveWorldEvent) {
        val leaver = Bukkit.getPlayer(e.entityUUID) ?: return
        val data = e.player.statusData()
        data.cacheJoiner.remove(leaver.uniqueId)
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        playerDataMap.remove(e.player.uniqueId)?.nowRunningState?.stop()
        // 清理节流 Map 中该玩家的数据
        keyPressThrottleMap.keys.removeIf { it.startsWith("${e.player.uniqueId}_") }
        // 清理玩家的隐藏手部任务
        playerInvisibleHandTaskMap.remove(e.player.uniqueId)?.let { SimpleTimeoutTask.cancel(it, false) }
    }

    @SubscribeEvent
    private fun respawn(e: PlayerRespawnEvent) {
        autoCheckStatus(e.player)
    }

    /**
     * 公共按键处理方法
     * @param player 玩家
     * @param key 按键
     */
    private fun handleKeyPress(player: Player, key: String) {
        // 节流检查：限制同一玩家同一按键的处理频率
        val throttleKey = "${player.uniqueId}_$key"
        val now = System.currentTimeMillis()
        val lastPress = keyPressThrottleMap[throttleKey] ?: 0L
        if (now - lastPress < THROTTLE_INTERVAL) return
        keyPressThrottleMap[throttleKey] = now

        val data = playerDataMap[player.uniqueId]
        if (data == null) {
            playerDataMap.getOrPut(player.uniqueId) { PlayerData(player) }
                .updateMoveState(key)
            return
        }

        data.updateMoveState(key)
        if (data.status != null) {
            data.tryNext(key)
        }
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: KeyPressEvent) {
        if (e.isCancelled) return
        handleKeyPress(e.player, e.key)
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: GermKeyDownEvent) {
        if (e.isCancelled) return
        val key = when (val simpleKey = e.keyType.simpleKey) {
            "MLEFT" -> MOUSE_LEFT
            "MRIGHT" -> MOUSE_RIGHT
            else -> simpleKey
        }
        handleKeyPress(e.player, key)
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: ClientKeyPressEvent) {
        handleKeyPress(e.player, e.keyName.uppercase())
    }

    @Ghost
    @SubscribeEvent
    private fun release(e: KeyReleaseEvent) {
        if (e.isCancelled) return
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        val running = data.nowRunningState as? PressGeneralAttackState.Running ?: return

        e.player.keySetting {
            if (it.generalAttackKey == e.key) {
                running.castAttack()
            }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun release(e: GermKeyUpEvent) {
        if (e.isCancelled) return
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        val running = data.nowRunningState as? PressGeneralAttackState.Running ?: return

        e.player.keySetting {
            val key = when (val simpleKey = e.keyType.simpleKey) {
                "MLEFT" -> MOUSE_LEFT
                "MRIGHT" -> MOUSE_RIGHT
                else -> simpleKey
            }
            if (it.generalAttackKey == key) {
                running.castAttack()
            }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun release(e: ClientKeyReleaseEvent) {
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        val running = data.nowRunningState as? PressGeneralAttackState.Running ?: return

        e.player.keySetting {
            if (it.generalAttackKey == e.keyName.uppercase()) {
                running.castAttack()
            }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun join(e: CustomPacketEvent) {
        if (e.identifier == "DragonCore" && e.data.size == 1 && e.data[0] == "cache_loaded") {
            autoCheckStatus(e.player)
        }
        if (e.identifier == "OrryxState") {
            when(e.data[0]) {
                "InvisibleHand" -> {
                    val invisible = e.data[1].cbool
                    if (invisible) {
                        val tick = e.data[2].clong
                        setInvisibleHand(e.player, tick)
                    } else {
                        cancelInvisibleHand(e.player)
                    }
                }
                "PlayHand" -> {
                    val animation = e.data[1]
                    val tick = e.data[2].clong
                    val speed = e.data[3].cfloat
                    setPlayHand(e.player, animation, tick, speed)
                }
                "stopAttack" -> {
                    e.player.keySetting {
                        KeyRegisterManager.getKeyRegister(e.player.uniqueId)?.keyRelease(it.generalAttackKey)
                    }
                }
            }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun join(e: ClientEntityJoinEvent) {
        val joiner = Bukkit.getPlayer(e.entityUUID) ?: return
        val joinerData = joiner.statusData()
        val data = e.player.statusData()
        data.cacheJoiner.add(joiner.uniqueId)
        val joinerStatus = joinerData.status as? Status ?: return
        joinerStatus.options.controller?.let { getController(it) }?.let { controller ->
            DragonCoreCustomPacketSender.setPlayerAnimationController(e.player, joiner.uniqueId, controller.saveToString())
        }
    }

    @Ghost
    @SubscribeEvent
    private fun leave(e: ClientEntityLeaveEvent) {
        val leaver = Bukkit.getPlayer(e.entityUUID) ?: return
        val data = e.player.statusData()
        data.cacheJoiner.remove(leaver.uniqueId)
    }

    @Ghost
    @SubscribeEvent
    private fun join(e: GermClientLinkedEvent) {
        autoCheckStatus(e.player)
    }

    @Ghost
    @SubscribeEvent
    private fun channel(e: ClientChannelEvent) {
        autoCheckStatus(e.player)
    }

    @SubscribeEvent
    private fun changeJob(e: OrryxPlayerJobChangeEvents.Post) {
        autoCheckStatus(e.player)
    }

    @SubscribeEvent
    private fun handle(e: PlayerItemHeldEvent) {
        val data = e.player.statusData()
        val status = data.status as? Status ?: return
        if (status.options.cancelHeldEventWhenPlaying && data.nowRunningState?.stop == false) {
            e.isCancelled = true
        }
    }

    @Ghost
    @SubscribeEvent
    private fun updateArmourers(e: PlayerSkinUpdateEvent) {
        val status = e.player.statusData().status as? Status ?: return
        val set = (status.options.getArmourers(e.player) + e.skinList).toSet()
        e.skinList.clear()
        e.skinList.addAll(set)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private fun damage(e: EntityDamageByEntityEvent) {
        val player = e.damager as? Player ?: return
        val status = player.statusData().status as? Status ?: return
        if (status.options.cancelBukkitAttack && (e.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) {
            e.isCancelled = true
        }
    }

    internal fun setPlayHand(player: Player, animation: String, tick: Long, speed: Float = 1.0f) {
        val players = player.statusData().cacheJoiner.mapNotNull { Bukkit.getPlayer(it) }
        playerInvisibleHandTaskMap[player.uniqueId]?.let { SimpleTimeoutTask.cancel(it, false) }
        playerInvisibleHandTaskMap[player.uniqueId] = SimpleTimeoutTask.createSimpleTask(tick) {
            DragonCoreCustomPacketSender.setEntityModelItemAnimation(player, player.uniqueId, animation, speed)
            players.forEach { target ->
                DragonCoreCustomPacketSender.setEntityModelItemAnimation(target, player.uniqueId, animation, speed)
            }
        }
    }

    internal fun setInvisibleHand(player: Player, tick: Long) {
        setPlayHand(player, "invisible", tick)
    }

    internal fun cancelInvisibleHand(player: Player) {
        val players = player.statusData().cacheJoiner.mapNotNull { Bukkit.getPlayer(it) }
        playerInvisibleHandTaskMap[player.uniqueId]?.let { SimpleTimeoutTask.cancel(it, false) }
        DragonCoreCustomPacketSender.setEntityModelItemAnimation(player, player.uniqueId, "idle", 1.0f)
        players.forEach { target ->
            DragonCoreCustomPacketSender.setEntityModelItemAnimation(target, player.uniqueId, "idle", 1.0f)
        }
    }

    fun autoCheckStatus(player: Player): CompletableFuture<Status?> {
        val data = playerDataMap.getOrPut(player.uniqueId) { PlayerData(player) }
        val future = CompletableFuture<Status?>()
        CompletableFuture.allOf(
            *statusMap.values.map { status ->
                status.options.getCondition(player).thenAccept { bool ->
                    val bool = bool.cbool
                    if (bool) {
                        ensureSync { data.setStatus(status) }
                        future.complete(status)
                    }
                }
            }.toTypedArray()
        ).whenComplete { _, _ ->
            if (!future.isDone) {
                ensureSync { data.setStatus(null) }
                future.complete(null)
            }
        }
        return future
    }

    fun callNext(player: Player): CompletableFuture<IRunningState?>? {
        val data = playerDataMap.getOrPut(player.uniqueId) { PlayerData(player) }
        return player.keySetting {
            it.bindKeyMap.forEach { (keyBind, mapping) ->
                if (mapping == data.nextInput) {
                    val result = keyBind.tryCast(player).getNow(CastResult.CANCELED)
                    if (result == CastResult.SUCCESS) {
                        return@keySetting null
                    }
                }
            }
            if (KeyRegisterManager.getKeyRegister(player.uniqueId)?.isKeyPress(it.generalAttackKey) == true) {
                data.tryNext(it.generalAttackKey)
            } else {
                data.tryNext(data.nextInput ?: return@keySetting null)
            }
        }.orNull()
    }

    fun loadScript(state: IActionState, action: String): Script? {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, state.key, getBytes(action), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            warning("State: ${state.key}")
            ex.printKetherErrorMessage()
            null
        }
    }

    fun loadScript(status: IStatus, action: String): Script? {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, status.key, getBytes(action), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            warning("Status: ${status.key}")
            ex.printKetherErrorMessage()
            null
        }
    }

    fun Player.statusData(): PlayerData {
        return playerDataMap.getOrPut(uniqueId) { PlayerData(this) }
    }

    fun statusDataList(): List<PlayerData> {
        return playerDataMap.values.toList()
    }

    fun getGlobalState(key: String): IActionState? {
        return globalState[key]
    }

    fun getController(key: String): Configuration? {
        return controllerMap[key]
    }
}