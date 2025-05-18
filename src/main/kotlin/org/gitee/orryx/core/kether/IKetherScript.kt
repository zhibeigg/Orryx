package org.gitee.orryx.core.kether

import org.gitee.orryx.core.kether.parameter.SkillParameter
import taboolib.module.kether.Script
import java.util.concurrent.CompletableFuture

interface IKetherScript {

    val script: Script

    fun runActions(skillParameter: SkillParameter, map: Map<String, Any>? = null): CompletableFuture<Any?>

    fun runExtendActions(skillParameter: SkillParameter, extend: String): CompletableFuture<Any?>
}