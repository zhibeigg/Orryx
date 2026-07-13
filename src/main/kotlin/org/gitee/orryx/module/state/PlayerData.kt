package org.gitee.orryx.module.state

import com.germ.germplugin.api.GermPacketAPI
import eos.moe.armourers.api.DragonAPI
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.module.spirit.SpiritDebitResult
import org.gitee.orryx.module.spirit.SpiritManagerDefault
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
    private val transitionLock = Any()
    private var transitionTail = CompletableFuture.completedFuture(Unit)

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
        if (status == null) return null
        val generation = transitionGeneration.incrementAndGet()
        return enqueueTransition {
            val currentStatus = status
                ?: return@enqueueTransition CompletableFuture.completedFuture(null)
            currentStatus.next(this, input).thenComposeMain { nextState ->
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
            val cost = (nextState.state as? ISpiritCost)?.spirit ?: 0.0
            require(cost.isFinite() && cost >= 0.0) { "状态精力消耗必须是非负有限数字" }
            val consumption = if (cost == 0.0) {
                CompletableFuture.completedFuture(SpiritDebitResult(SpiritResult.SUCCESS, 0.0))
            } else {
                ISpiritManager.INSTANCE.takeSpiritDetailed(player, cost)
            }
            consumption.thenComposeMain { debit ->
                val currentGeneration = generation == transitionGeneration.get()
                val currentState = nowRunningState === current
                if (debit.result != SpiritResult.SUCCESS) {
                    if (currentGeneration) nextInput = input
                    return@thenComposeMain CompletableFuture.completedFuture(null)
                }
                if (!currentGeneration || !currentState) {
                    return@thenComposeMain refundSpirit(debit.amount, null).thenApply { null }
                }
                try {
                    commitPreparedState(nextState, advanceGeneration = false)
                    CompletableFuture.completedFuture(nextState)
                } catch (throwable: Throwable) {
                    if (generation == transitionGeneration.get()) nextInput = input
                    refundSpirit(debit.amount, throwable).thenApply { null }
                }
            }
        }
        }
    }

    private fun <T> enqueueTransition(operation: () -> CompletableFuture<T>): CompletableFuture<T> {
        val result: CompletableFuture<T>
        synchronized(transitionLock) {
            val ready = transitionTail.handle { _, _ -> Unit }
            result = ready.thenCompose {
                try {
                    operation()
                } catch (throwable: Throwable) {
                    CompletableFuture<T>().also { it.completeExceptionally(throwable) }
                }
            }
            transitionTail = result.handle { _, _ -> Unit }
        }
        return result
    }

    private fun refundSpirit(amount: Double, originalFailure: Throwable?): CompletableFuture<Unit> {
        if (amount <= 0.0) {
            if (originalFailure == null) return CompletableFuture.completedFuture(Unit)
            return CompletableFuture<Unit>().also { it.completeExceptionally(originalFailure) }
        }
        val refund = (ISpiritManager.INSTANCE as? SpiritManagerDefault)?.refundSpiritExact(player, amount)
            ?: ISpiritManager.INSTANCE.giveSpirit(player, amount).thenApply { result ->
                check(result == SpiritResult.SUCCESS) { "精力补偿失败: $result" }
                Unit
            }
        if (originalFailure == null) return refund
        return refund.handle { _, refundFailure ->
            if (refundFailure != null) originalFailure.addSuppressed(refundFailure)
            throw java.util.concurrent.CompletionException(originalFailure)
        }
    }

    /**
     * 强制执行下一个状态
     * @param running 用于读取下一操作的输入键
     * @return 过渡到的状态
     * */
    fun next(running: IRunningState) {
        commitPreparedState(running)
    }

    /** 技能启动提交专用：新状态成功启动前不停止旧状态。 */
    internal fun commitPreparedState(running: IRunningState, advanceGeneration: Boolean = true) {
        if (advanceGeneration) transitionGeneration.incrementAndGet()
        val previous = nowRunningState
        nowRunningState = running
        try {
            running.start()
        } catch (throwable: Throwable) {
            nowRunningState = previous
            throw throwable
        }
        try {
            previous?.stop()
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
        nextInput = null
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