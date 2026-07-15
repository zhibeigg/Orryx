package org.gitee.orryx.core.editor.handler

/** 会修改工作区或触发配置激活的普通 Editor 操作。 */
enum class EditorMutationOperation {
    FILE_WRITE,
    FILE_CREATE,
    FILE_DELETE,
    FILE_RENAME,
    RELOAD,
}

/**
 * 发布磁盘交换与 readiness/rollback 期间的普通 mutation gate。
 *
 * gate 只保存发布事务的最小安全状态；恢复扫描会重新登记未完成事务，RECOVERY_REQUIRED 必须由后续
 * 人工/协议恢复明确释放，不能因进程重启自动放行普通写入。
 */
class EditorMutationGate {

    data class Blocker(
        val transactionId: String,
        val state: String,
    )

    class MutationBlockedException(
        val blocker: Blocker,
        operation: EditorMutationOperation,
    ) : Exception(
        "发布事务 ${blocker.transactionId} 处于 ${blocker.state}，暂不允许 ${operation.name}",
    )

    private val lock = Any()
    private val blockers = linkedMapOf<String, String>()

    /** 开始发布 gate；若已有其他发布事务占用则返回该 blocker。 */
    fun begin(transactionId: String, state: String): Blocker? = synchronized(lock) {
        val conflict = blockers.entries.firstOrNull { it.key != transactionId }
        if (conflict != null) {
            Blocker(conflict.key, conflict.value)
        } else {
            blockers[transactionId] = state
            null
        }
    }

    /** 更新或恢复事务 gate，不会隐式释放其他恢复事务。 */
    fun hold(transactionId: String, state: String) {
        synchronized(lock) {
            blockers[transactionId] = state
        }
    }

    fun release(transactionId: String) {
        synchronized(lock) {
            blockers.remove(transactionId)
        }
    }

    fun checkAllowed(operation: EditorMutationOperation) {
        val blocker = synchronized(lock) {
            blockers.entries.firstOrNull()?.let { Blocker(it.key, it.value) }
        }
        if (blocker != null) throw MutationBlockedException(blocker, operation)
    }

    fun currentBlockers(): List<Blocker> = synchronized(lock) {
        blockers.map { Blocker(it.key, it.value) }
    }

    companion object {
        val shared = EditorMutationGate()
    }
}
