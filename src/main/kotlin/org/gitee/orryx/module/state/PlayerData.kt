package org.gitee.orryx.module.state

import com.germ.germplugin.api.GermPacketAPI
import eos.moe.armourers.api.DragonAPI
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.module.spirit.SpiritResult
import org.gitee.orryx.utils.thenApplyMain
import org.gitee.orryx.utils.thenComposeMain
import org.gitee.orryx.module.state.StateManager.getController
import org.gitee.orryx.module.state.StateManager.statusDataList
import org.gitee.orryx.utils.ArcartXPlugin
import org.gitee.orryx.utils.DragonArmourersPlugin
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin
import priv.seventeen.artist.arcartx.internal.network.NetworkMessageSender
import taboolib.common.platform.function.warning
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

class PlayerData(val player: Player) {

    var status: IStatus? = null
        private set

    private var changeMoveState = MoveState.FRONT

    val cacheJoiner = linkedSetOf<UUID>()

    //移动方向
    val moveState: MoveState
        get() = MoveState.entries.firstOrNull {
            KeyRegisterManager.getKeyRegister(player.uniqueId)?.isKeyPress(it.key) == true
        } ?: changeMoveState

    //预输入
    var nextInput: String? = null

    //现在的运行状态
    var nowRunningState: IRunningState? = null
        private set

    private val transitionGeneration = AtomicLong()

    fun firstCheck(@Suppress("UNUSED_PARAMETER") runningState: IRunningState): Boolean {
        if (nowRunningState != null) return false
        return player.gameMode != GameMode.SPECTATOR && player.gameMode != GameMode.CREATIVE
    }

    /**
     * 尝试执行下一个状态
     * @param input 用于读取下一操作的输入键
     * @return 过渡到的状态
     * */
    fun tryNext(input: String): CompletableFuture<IRunningState?>? {
        val generation = transitionGeneration.incrementAndGet()
        return status?.next(this, input)?.thenComposeMain { nextState ->
            if (generation != transitionGeneration.get()) {
                return@thenComposeMain CompletableFuture.completedFuture(null)
            }
            if (nextState == null) {
                nextInput = input
                return@thenComposeMain CompletableFuture.completedFuture(null)
            }
            val current = nowRunningState
            val allowed = (current == null && firstCheck(nextState)) || current?.hasNext(nextState) == true
            if (!allowed) {
                nextInput = input
                return@thenComposeMain CompletableFuture.completedFuture(null)
            }
            val cost = (nextState.state as? ISpiritCost)?.spirit?.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            val consumption = if (cost <= 0.0) {
                CompletableFuture.completedFuture(SpiritResult.SUCCESS)
            } else {
                ISpiritManager.INSTANCE.takeSpirit(player, cost)
            }
            consumption.thenApplyMain { result ->
                if (result != SpiritResult.SUCCESS || generation != transitionGeneration.get() || nowRunningState !== current) {
                    nextInput = input
                    return@thenApplyMain null
                }
                current?.stop()
                nextInput = null
                nowRunningState = nextState
                nextState.start()
                nextState
            }
        }
    }

    /**
     * 强制执行下一个状态
     * @param running 用于读取下一操作的输入键
     * @return 过渡到的状态
     * */
    fun next(running: IRunningState) {
        transitionGeneration.incrementAndGet()
        nowRunningState?.stop()
        nextInput = null
        nowRunningState = running
        running.start()
    }

    fun clearRunningState() {
        nowRunningState = null
    }

    fun setStatus(status: Status?) {
        if (this.status == status) return
        transitionGeneration.incrementAndGet()
        nowRunningState?.stop()
        nowRunningState = null
        this.status = status
        if (status != null) {
            updateController(status)
        } else {
            removeController()
        }
        if (dragonArmourersEnabled) {
            DragonAPI.updatePlayerSkin(player)
        }
        if (germEnabled) {
            updateAnimationState(status ?: return)
            updateGermSkin(status)
        }
    }

    // 控制器更新
    fun updateController(status: Status) {
        val controller = status.options.controller?.let { getController(it) } ?: return
        val controllerString = controller.saveToString()
        when {
            dragonCoreEnabled -> {
                DragonCoreCustomPacketSender.setPlayerAnimationController(player, player.uniqueId, controllerString)
                statusDataList().forEach {
                    if (player.uniqueId in it.cacheJoiner) {
                        DragonCoreCustomPacketSender.setPlayerAnimationController(it.player, player.uniqueId, controllerString)
                    }
                }
            }
            arcartXEnabled -> {
                NetworkMessageSender.sendSetController(player, player.uniqueId, controllerString)
                statusDataList().forEach {
                    if (player.uniqueId in it.cacheJoiner) {
                        NetworkMessageSender.sendSetController(it.player, player.uniqueId, controllerString)
                    }
                }
            }
        }
    }

    // 控制器删除
    fun removeController() {
        when {
            dragonCoreEnabled -> {
                DragonCoreCustomPacketSender.removePlayerAnimationController(player, player.uniqueId)
                statusDataList().forEach {
                    if (player.uniqueId in it.cacheJoiner) {
                        DragonCoreCustomPacketSender.removePlayerAnimationController(it.player, player.uniqueId)
                    }
                }
            }
            arcartXEnabled -> {
                NetworkMessageSender.sendSetController(player, player.uniqueId, "")
                statusDataList().forEach {
                    if (player.uniqueId in it.cacheJoiner) {
                        NetworkMessageSender.sendSetController(it.player, player.uniqueId, "")
                    }
                }
            }
        }
    }

    // 萌芽的动作状态更新
    fun updateAnimationState(status: Status) {
        if (!germEnabled) return
        val animationState = status.options.animationState ?: return
        onlinePlayers.forEach { viewer ->
            GermPacketAPI.changeEntityModelAnimationState(viewer, player.entityId, animationState)
        }
    }

    fun updateGermSkin(status: Status) {
        if (status.options.getArmourers(player).isNotEmpty()) {
            warning("Orryx 暂时未能支持 GermPlugin 的 Status 时装")
        }
    }

    fun updateMoveState(input: String) {
        when(input) {
            MoveState.FRONT.key -> changeMoveState = MoveState.FRONT
            MoveState.REAR.key -> changeMoveState = MoveState.REAR
            MoveState.LEFT.key -> changeMoveState = MoveState.LEFT
            MoveState.RIGHT.key -> changeMoveState = MoveState.RIGHT
        }
    }

    fun getAttackSpeed(): Float {
        return (status as? Status)?.options?.getAttackSpeed(player) ?: 1.0f
    }

    companion object {

        val germEnabled: Boolean by lazy { GermPluginPlugin.isEnabled }
        val dragonCoreEnabled: Boolean by lazy { DragonCorePlugin.isEnabled }
        val arcartXEnabled: Boolean by lazy { ArcartXPlugin.isEnabled }
        val dragonArmourersEnabled: Boolean by lazy { DragonArmourersPlugin.isEnabled }
    }
}