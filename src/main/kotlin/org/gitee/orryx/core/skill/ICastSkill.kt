package org.gitee.orryx.core.skill

import taboolib.module.kether.Script

/**
 * 可释放技能接口。
 *
 * @property actions 技能运行脚本
 * @property extendActions 技能拓展运行脚本
 * @property castCheckAction 技能运行前检测脚本
 * @property script 技能运行脚本实例
 * @property extendScripts 技能拓展子脚本
 */
interface ICastSkill: ISkill {

    val actions: String

    val extendActions: Map<String, String>

    val castCheckAction: String?

    val script: Script?

    val extendScripts: Map<String, Script?>
}
