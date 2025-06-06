package org.gitee.orryx.module.state

import com.germ.germplugin.api.event.GermClientLinkedEvent
import com.germ.germplugin.api.event.GermKeyDownEvent
import eos.moe.armourers.api.PlayerSkinUpdateEvent
import eos.moe.armourers.nu
import eos.moe.dragoncore.api.event.EntityJoinWorldEvent
import eos.moe.dragoncore.api.event.EntityLeaveWorldEvent
import eos.moe.dragoncore.api.event.KeyPressEvent
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
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
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.common5.cbool
import taboolib.common5.cfloat
import taboolib.common5.clong
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptService
import taboolib.module.kether.orNull
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.CompletableFuture

object StateManager {

    private val playerDataMap = hashMapOf<UUID, PlayerData>()
    private val statusMap = hashMapOf<String, Status>()
    private val controllerMap = hashMapOf<String, Configuration>()
    private val globalState = hashMapOf<String, IActionState>()

    private val playerInvisibleHandTaskMap by unsafeLazy { hashMapOf<UUID, SimpleTimeoutTask>() }

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
        info("&e┣&7GlobalState loaded &e${globalState.size} &a√".colored())
    }

    @Awake(LifeCycle.ENABLE)
    @Reload(2)
    private fun reload() {
        controllerMap.clear()
        files("controllers", "example.yml") { file ->
            controllerMap[file.nameWithoutExtension] = Configuration.loadFromFile(file)
        }
        info("&e┣&7Controllers loaded &e${controllerMap.size} &a√".colored())
        statusMap.clear()
        files("status", "example.yml") { file ->
            statusMap[file.nameWithoutExtension] = Status(file.nameWithoutExtension, Configuration.loadFromFile(file))
        }
        info("&e┣&7Status loaded &e${statusMap.size} &a√".colored())
        onlinePlayers.forEach {
            autoCheckStatus(it)
        }
    }

    fun load(key: String, configurationSection: ConfigurationSection): IActionState {
        return when (val type = configurationSection.getString("Type")?.uppercase()) {
            BLOCK_STATE -> BlockState(key, configurationSection)
            DODGE_STATE -> DodgeState(key, configurationSection)
            GENERAL_ATTACK_STATE -> GeneralAttackState(key, configurationSection)
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
    }

    @SubscribeEvent
    private fun respawn(e: PlayerRespawnEvent) {
        autoCheckStatus(e.player)
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: KeyPressEvent) {
        if (e.isCancelled) return
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        data.updateMoveState(e.key)
        data.tryNext(e.key)
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: GermKeyDownEvent) {
        if (e.isCancelled) return
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        data.updateMoveState(e.keyType.simpleKey)
        data.tryNext(e.keyType.simpleKey)
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
            }
        }
    }

    @Ghost
    @SubscribeEvent
    private fun join(e: GermClientLinkedEvent) {
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
        e.skinList.addAll(status.options.getArmourers(e.player))
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private fun damage(e: EntityDamageByEntityEvent) {
        val player = e.damager as? Player ?: return
        val status = player.statusData().status as? Status ?: return
        if (status.options.cancelBukkitAttack) {
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
                player.eval(status.options.conditionAction, emptyMap()).thenAccept { bool ->
                    val bool = bool.cbool
                    if (bool) {
                        data.setStatus(status)
                        future.complete(status)
                    }
                }
            }.toTypedArray()
        ).whenComplete { _, _ ->
            if (!future.isDone) {
                data.setStatus(null)
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