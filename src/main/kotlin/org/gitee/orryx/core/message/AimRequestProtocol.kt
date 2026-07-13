package org.gitee.orryx.core.message

import java.util.concurrent.atomic.AtomicReference

/**
 * 保持 legacy 二进制字段顺序不变，并通过一次性 wire skillId 关联单次瞄准请求。
 *
 * 官方客户端会原样回传请求中的 skillId，因此无需新增数据包字段或修改 header。
 */
internal object AimRequestProtocol {

    const val MAX_SKILL_ID_LENGTH = 256
    private const val TOKEN_SEPARATOR = "~orryx~"

    fun createWireSkillId(skillId: String, requestId: Long): String {
        require(skillId.isNotBlank()) { "技能标识不能为空" }
        require(requestId > 0L) { "瞄准请求标识必须大于 0" }
        val suffix = TOKEN_SEPARATOR + requestId.toString(36)
        require(suffix.length < MAX_SKILL_ID_LENGTH) { "瞄准请求标识过长" }
        return skillId.take(MAX_SKILL_ID_LENGTH - suffix.length) + suffix
    }
}

internal enum class AimRequestPhase {
    CREATED,
    CONFIRMED,
    COMPLETED,
}

/** 单次瞄准请求只允许 CREATED → CONFIRMED → COMPLETED。 */
internal class AimRequestLifecycle {

    private val phase = AtomicReference(AimRequestPhase.CREATED)

    fun confirm(): Boolean {
        return phase.compareAndSet(AimRequestPhase.CREATED, AimRequestPhase.CONFIRMED)
    }

    fun isConfirmed(): Boolean = phase.get() == AimRequestPhase.CONFIRMED

    fun complete(): Boolean {
        return phase.compareAndSet(AimRequestPhase.CONFIRMED, AimRequestPhase.COMPLETED)
    }

    fun cancel(): Boolean {
        return phase.getAndSet(AimRequestPhase.COMPLETED) != AimRequestPhase.COMPLETED
    }

    internal fun currentPhase(): AimRequestPhase = phase.get()
}
