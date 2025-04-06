package org.gitee.orryx.module.state

import com.germ.germplugin.api.event.GermClientLinkedEvent
import com.germ.germplugin.api.event.GermKeyDownEvent
import eos.moe.dragoncore.api.event.KeyPressEvent
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.core.reload.Reload
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
import taboolib.common5.cbool
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptService
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.CompletableFuture

object StateManager {

    private val playerDataMap = hashMapOf<UUID, PlayerData>()
    private val statusMap = hashMapOf<String, Status>()
    private val controllerMap = hashMapOf<String, Configuration>()
    private val globalState = hashMapOf<String, IActionState>()

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
        statusMap.clear()
        files("status", "example.yml") { file ->
            statusMap[file.nameWithoutExtension] = Status(file.nameWithoutExtension, Configuration.loadFromFile(file))
        }
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

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        playerDataMap.remove(e.player.uniqueId)
    }

    @Ghost
    @SubscribeEvent(EventPriority.LOWEST, ignoreCancelled = false)
    private fun move(e: KeyPressEvent) {
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        data.updateMoveState(e.key)
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: KeyPressEvent) {
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        data.tryNext(e.key)
    }

    @Ghost
    @SubscribeEvent(EventPriority.LOWEST, ignoreCancelled = false)
    private fun move(e: GermKeyDownEvent) {
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        data.updateMoveState(e.keyType.simpleKey)
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: GermKeyDownEvent) {
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        data.tryNext(e.keyType.simpleKey)
    }

    @Ghost
    @SubscribeEvent
    private fun join(e: CustomPacketEvent) {
        if (e.identifier == "DragonCore" && e.data.size == 1 && e.data[0] == "cache_loaded") {
            autoCheckStatus(e.player)
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

    fun autoCheckStatus(player: Player) {
        val data = playerDataMap.getOrPut(player.uniqueId) { PlayerData(player) }
        val status = statusMap.values.firstOrNull {
            player.eval(it.options.conditionAction, emptyMap()).getNow(false).cbool
        }
        data.setStatus(status)
    }

    fun callNext(player: Player): CompletableFuture<IRunningState?>? {
        val data = playerDataMap.getOrPut(player.uniqueId) { PlayerData(player) }
        return data.nextInput?.let {
            data.tryNext(it)
        } ?: player.keySetting {
            if (KeyRegisterManager.getKeyRegister(player.uniqueId)?.isKeyPress(it.generalAttackKey) == true) {
                data.tryNext(it.generalAttackKey)
            } else {
                null
            }
        }.getNow(null)
    }

    fun loadScript(state: IActionState, action: String): Script? {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, state.key, getBytes(action), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            ex.printStackTrace()
            warning("State: ${state.key}")
            null
        }
    }

    fun loadScript(status: IStatus, action: String): Script {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, status.key, getBytes(action), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            ex.printStackTrace()
            error("Status: ${status.key}")
        }
    }

    fun Player.statusData(): PlayerData {
        return playerDataMap.getOrPut(uniqueId) { PlayerData(this) }
    }

    fun getGlobalState(key: String): IActionState? {
        return globalState[key]
    }

}