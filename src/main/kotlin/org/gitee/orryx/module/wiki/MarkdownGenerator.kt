package org.gitee.orryx.module.wiki

import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import java.io.File

object MarkdownGenerator {

    fun generate(outputFile: File) {
        val actionGroup = ScriptManager.wikiActions.groupBy { it.group }
        val selectorsGroup = ScriptManager.wikiSelectors.groupBy { it.type }
        val triggersGroup = ScriptManager.wikiTriggers.groupBy { it.group }
        val propertiesGroup = ScriptManager.wikiProperties.groupBy { it.group }

        val sb = StringBuilder()
        sb.appendLine("# $pluginId-$pluginVersion 脚本语句文档（自生成）")
        sb.appendLine()

        // 说明
        appendIntro(sb)

        // 技能YAML配置结构
        appendSkillYamlStructure(sb)

        // Actions
        actionGroup.forEach { (group, actions) ->
            appendActionGroup(sb, group, actions)
        }

        // Selectors
        appendSelectorSection(sb, selectorsGroup)

        // Triggers
        appendTriggerSection(sb, triggersGroup)

        // Properties
        appendPropertySection(sb, propertiesGroup)

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(sb.toString(), Charsets.UTF_8)
    }

    private fun appendIntro(sb: StringBuilder) {
        sb.appendLine("更多原生Kether语句请查看 https://kether.tabooproject.org/list.html")
        sb.appendLine()
        sb.appendLine("符号说明: `[*]` 代表可选 `<*>` 代表必选 `()` 代表默认值 前缀`*`代表先导词")
        sb.appendLine()
    }

    private fun appendSkillYamlStructure(sb: StringBuilder) {
        sb.appendLine("# 技能YAML配置结构")
        sb.appendLine()
        sb.appendLine("## 通用字段（所有技能类型）")
        sb.appendLine()
        sb.appendLine("```yaml")
        sb.appendLine("Options:")
        sb.appendLine("  Type: \"Direct\"          # 技能类型: Direct(直接) | Direct Aim(指向性) | Pressing(蓄力) | Pressing Aim(蓄力指向性) | Passive(被动)")
        sb.appendLine("  Name: \"技能名称\"         # 显示名称（默认为文件名）")
        sb.appendLine("  Sort: 0                  # UI排序位置")
        sb.appendLine("  Icon: \"图标\"             # HUD图标（默认为Name）")
        sb.appendLine("  XMaterial: \"BLAZE_ROD\"   # 物品材质类型")
        sb.appendLine("  Description:             # 技能描述（*开头不预览下一级，{{}}内语句二级预览）")
        sb.appendLine("    - '&f技能等级&7: &e{{ &level }} &f级'")
        sb.appendLine("  IsLocked: false          # 是否需要解锁")
        sb.appendLine("  MinLevel: 1              # 最小等级")
        sb.appendLine("  MaxLevel: 5              # 最高等级")
        sb.appendLine("  UpgradePointAction: 1    # 升级消耗技能点（Kether表达式）")
        sb.appendLine("  UpLevelCheckAction: |-   # 升级检查（Kether脚本，返回Boolean）")
        sb.appendLine("    check orryx level >= calc \"2+2*(to-1)\"")
        sb.appendLine("  UpLevelSuccessAction: |- # 升级成功后执行（Kether脚本）")
        sb.appendLine("    tell \"升级成功\"")
        sb.appendLine("  IgnoreSilence: false     # 是否无视沉默")
        sb.appendLine("  Variables:               # 自定义变量（键自动转大写）")
        sb.appendLine("    Silence: 5             # 释放后沉默时间(tick)")
        sb.appendLine("    Mana: 5                # 法力消耗")
        sb.appendLine("    Cooldown: 5            # 冷却时间(tick)")
        sb.appendLine("```")
        sb.appendLine()
        sb.appendLine("## 可释放技能字段（Direct、Direct Aim、Pressing、Pressing Aim）")
        sb.appendLine()
        sb.appendLine("```yaml")
        sb.appendLine("Options:")
        sb.appendLine("  CastCheckAction: true    # 释放前检查（Kether脚本，返回Boolean）")
        sb.appendLine("Actions: |-                # 技能主逻辑（Kether脚本，#开头的行被过滤）")
        sb.appendLine("  damage 10 they \"@range 5 !@self\"")
        sb.appendLine("ExtendActions:             # 扩展动作（命名的Kether脚本）")
        sb.appendLine("  完成: |-")
        sb.appendLine("    tell \"完成\"")
        sb.appendLine("```")
        sb.appendLine()
        sb.appendLine("## 指向性技能字段（Direct Aim）")
        sb.appendLine()
        sb.appendLine("```yaml")
        sb.appendLine("Options:")
        sb.appendLine("  AimSizeAction: \"5\"       # 指示范围大小（Kether表达式）")
        sb.appendLine("  AimRadiusAction: \"10\"    # 指示原点最大半径（Kether表达式）")
        sb.appendLine("```")
        sb.appendLine()
        sb.appendLine("## 蓄力技能字段（Pressing、Pressing Aim）")
        sb.appendLine()
        sb.appendLine("```yaml")
        sb.appendLine("Options:")
        sb.appendLine("  Period: 10               # 蓄力周期（tick）")
        sb.appendLine("  MaxPressTickAction: \"20\" # 最大蓄力时间（Kether表达式）")
        sb.appendLine("  PressPeriodAction: |-    # 蓄力每周期执行（Kether脚本，可用&pressTick变量）")
        sb.appendLine("    tell &pressTick")
        sb.appendLine("  PressBrockTriggers:      # 蓄力打断触发器列表")
        sb.appendLine("    - \"Player Damaged Post\"")
        sb.appendLine("```")
        sb.appendLine()
        sb.appendLine("## 蓄力指向性技能字段（Pressing Aim）")
        sb.appendLine()
        sb.appendLine("```yaml")
        sb.appendLine("Options:")
        sb.appendLine("  AimMinAction: \"5\"        # 指示范围初始大小（Kether表达式）")
        sb.appendLine("  AimMaxAction: \"10\"       # 指示范围最大大小（Kether表达式）")
        sb.appendLine("  AimRadiusAction: \"10\"    # 指示原点最大半径（Kether表达式）")
        sb.appendLine("```")
        sb.appendLine()
    }

    private fun appendActionGroup(sb: StringBuilder, group: String, actions: List<Action>) {
        sb.appendLine("# $group")
        sb.appendLine()
        val keyGroup = actions.groupBy { it.key }
        keyGroup.forEach { (key, actionList) ->
            sb.appendLine("## $key")
            sb.appendLine()
            actionList.forEach { action ->
                appendAction(sb, action)
            }
        }
    }

    private fun appendAction(sb: StringBuilder, action: Action) {
        val scope = if (action.sharded) "公有语句" else "私有语句"
        sb.appendLine("### ${action.name}（$scope）")
        sb.appendLine()

        // 语法行
        val line = buildActionSyntax(action)
        sb.appendLine("> `$line`")
        sb.appendLine()

        // 参数表
        if (action.entries.isNotEmpty()) {
            sb.appendLine("| 先导词 | 类型 | 可选 | 描述 |")
            sb.appendLine("|--------|------|------|------|")
            action.entries.forEach { entry ->
                sb.appendLine("| ${entry.head ?: "无"} | ${entry.type.name} | ${entry.optional} | ${entry.description} |")
            }
            sb.appendLine()
        }

        // 返回值
        if (action.result != Type.NULL) {
            sb.appendLine("| 返回值类型 | 描述 |")
            sb.appendLine("|-----------|------|")
            sb.appendLine("| ${action.result.name} | ${action.resultDescription ?: "无"} |")
            sb.appendLine()
        }

        // 描述
        if (action.description.isNotBlank()) {
            sb.appendLine("> ${action.description}")
            sb.appendLine()
        }

        // 示例
        if (action.example.isNotEmpty()) {
            sb.appendLine("```yaml")
            action.example.forEach { sb.appendLine(it) }
            sb.appendLine("```")
            sb.appendLine()
        }
    }

    private fun buildActionSyntax(action: Action): String {
        return "${action.key} " + action.entries.joinToString(" ") { entry ->
            val (start, end) = if (entry.optional) "[" to "]" else "<" to ">"
            if (entry.type == Type.SYMBOL) {
                entry.head!!
            } else {
                var s = "$start${entry.type.name}"
                if (entry.head != null) s = "*${entry.head} $s"
                if (entry.default != null) s += "(${entry.default})"
                s + end
            }
        }
    }

    private fun appendSelectorSection(sb: StringBuilder, groups: Map<SelectorType, List<Selector>>) {
        sb.appendLine("# Selector选择器列表")
        sb.appendLine()
        groups.forEach { (type, selectors) ->
            val typeName = when (type) {
                SelectorType.STREAM -> "流式选择器"
                SelectorType.GEOMETRY -> "几何选择器"
            }
            sb.appendLine("## $typeName")
            sb.appendLine()
            selectors.forEach { selector ->
                appendSelector(sb, selector)
            }
        }
    }

    private fun appendSelector(sb: StringBuilder, selector: Selector) {
        sb.appendLine("### ${selector.name}")
        sb.appendLine()

        val line = "@${selector.keys.joinToString("/")} " + selector.entries.joinToString(" ") { entry ->
            var s = "[${entry.type.name}"
            if (entry.default != null) s += "(${entry.default})"
            s + "]"
        }
        sb.appendLine("> `$line`")
        sb.appendLine()

        if (selector.entries.isNotEmpty()) {
            sb.appendLine("| 类型 | 描述 |")
            sb.appendLine("|------|------|")
            selector.entries.forEach { entry ->
                sb.appendLine("| ${entry.type.name} | ${entry.description} |")
            }
            sb.appendLine()
        }

        if (selector.description.isNotBlank()) {
            sb.appendLine("> ${selector.description}")
            sb.appendLine()
        }

        if (selector.example.isNotEmpty()) {
            selector.example.forEach { sb.appendLine("> $it") }
            sb.appendLine()
        }
    }

    private fun appendTriggerSection(sb: StringBuilder, groups: Map<TriggerGroup, List<Trigger>>) {
        sb.appendLine("# Trigger触发器列表")
        sb.appendLine()
        groups.forEach { (group, triggers) ->
            sb.appendLine("## ${group.value}")
            sb.appendLine()
            triggers.forEach { trigger ->
                appendTrigger(sb, trigger)
            }
        }
    }

    private fun appendTrigger(sb: StringBuilder, trigger: Trigger) {
        sb.appendLine("### ${trigger.key}")
        sb.appendLine()

        if (trigger.entries.isNotEmpty()) {
            sb.appendLine("| 类型 | Key | Value |")
            sb.appendLine("|------|-----|-------|")
            trigger.entries.forEach { entry ->
                sb.appendLine("| ${entry.type.name} | ${entry.key} | ${entry.description} |")
            }
            sb.appendLine()
        }

        if (trigger.description.isNotBlank()) {
            sb.appendLine("> ${trigger.description}")
            sb.appendLine()
        }

        if (trigger.specialKeyEntries.isNotEmpty()) {
            trigger.specialKeyEntries.forEach { entry ->
                sb.appendLine("> 特殊配置Key：(${entry.key})，类型：(${entry.type.name})，介绍：(${entry.description})")
            }
            sb.appendLine()
        }
    }

    private fun appendPropertySection(sb: StringBuilder, groups: Map<String, List<Property>>) {
        sb.appendLine("# Property属性列表")
        sb.appendLine()
        groups.forEach { (group, properties) ->
            sb.appendLine("## $group")
            sb.appendLine()
            properties.forEach { property ->
                appendProperty(sb, property)
            }
        }
    }

    private fun appendProperty(sb: StringBuilder, property: Property) {
        sb.appendLine("### ${property.name}")
        sb.appendLine()
        sb.appendLine("> 用法: `&变量名[key]` | ID: `${property.id}`")
        sb.appendLine()

        if (property.entries.isNotEmpty()) {
            sb.appendLine("| 属性Key | 类型 | 可写 | 描述 |")
            sb.appendLine("|---------|------|------|------|")
            property.entries.forEach { entry ->
                sb.appendLine("| ${entry.key} | ${entry.type.name} | ${if (entry.writable) "是" else "否"} | ${entry.description} |")
            }
            sb.appendLine()
        }

        if (property.description.isNotBlank()) {
            sb.appendLine("> ${property.description}")
            sb.appendLine()
        }
    }
}
