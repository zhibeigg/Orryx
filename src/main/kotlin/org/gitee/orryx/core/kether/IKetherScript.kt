package org.gitee.orryx.core.kether

import org.gitee.orryx.core.kether.parameter.SkillParameter
import taboolib.module.kether.Script

interface IKetherScript {

    val script: Script

    fun runActions(skillParameter: SkillParameter, map: Map<String, Any>? = null)

}