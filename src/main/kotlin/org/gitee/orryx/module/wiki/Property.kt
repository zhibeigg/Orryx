package org.gitee.orryx.module.wiki

import com.lark.oapi.service.docx.v1.enums.BlockBlockTypeEnum
import com.lark.oapi.service.docx.v1.enums.CalloutCalloutBackgroundColorEnum
import com.lark.oapi.service.docx.v1.enums.CalloutCalloutBorderColorEnum
import com.lark.oapi.service.docx.v1.enums.TextStyleAlignEnum
import com.lark.oapi.service.docx.v1.model.*
import taboolib.common.util.unsafeLazy

/**
 * ScriptProperty 文档模型
 *
 * @param group 分组名
 * @param name 属性名称（如 IPlayerJob）
 * @param id 属性ID（如 orryx.player.job.operator）
 * @param description 描述
 */
class Property(
    val group: String,
    val name: String,
    val id: String,
    var description: String = ""
): WikiBlock {

    private val entries by unsafeLazy { mutableListOf<Entry>() }

    /**
     * 属性条目
     *
     * @param key 属性键名
     * @param type 返回值类型
     * @param description 描述
     * @param writable 是否可写
     */
    class Entry(val key: String, val type: Type, val description: String, val writable: Boolean = false)

    companion object {

        fun new(group: String, name: String, id: String): Property {
            return Property(group, name, id)
        }

    }

    fun addEntry(key: String, type: Type, description: String, writable: Boolean = false): Property {
        entries += Entry(key, type, description, writable)
        return this
    }

    fun description(description: String): Property {
        this.description = description
        return this
    }

    override fun createBlocks(): Pair<List<Block>, List<String>> {
        val list = mutableListOf<Block>()
        val ids = mutableListOf<String>()

        fun text(text: String, isHead: Boolean = false): Array<TextElement> {
            return arrayOf(
                TextElement.newBuilder()
                    .textRun(
                        TextRun.newBuilder()
                            .textElementStyle(TextElementStyle.newBuilder().bold(isHead).build())
                            .content(text)
                            .build()
                    ).build()
            )
        }

        // 标题
        ids += "${name}_heading3"
        list += Block.newBuilder()
            .blockId("${name}_heading3")
            .children(arrayOf())
            .blockType(BlockBlockTypeEnum.HEADING3)
            .heading3(Text.newBuilder().elements(text(name)).build())
            .build()

        // Callout 显示用法
        val usage = "&变量名[key]"
        ids += "${name}_callout"
        list += Block.newBuilder()
            .blockId("${name}_callout")
            .blockType(BlockBlockTypeEnum.CALLOUT)
            .callout(
                Callout.newBuilder()
                    .emojiId("key")
                    .borderColor(CalloutCalloutBorderColorEnum.BLUE)
                    .backgroundColor(CalloutCalloutBackgroundColorEnum.LIGHTBLUE)
                    .build()
            )
            .children(arrayOf("${name}_callout_text"))
            .build()
        list += Block.newBuilder()
            .blockId("${name}_callout_text")
            .children(arrayOf())
            .blockType(BlockBlockTypeEnum.TEXT)
            .text(Text.newBuilder().elements(text("用法: $usage  |  ID: $id")).build())
            .build()

        // 属性表格
        var n = 1
        fun entriesCell(entry: Entry?, isHead: Boolean): List<String> {
            val cellIds = mutableListOf<String>()

            // Key 列
            cellIds += "${name}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_entries_cell_${n}_key"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_${n}_key")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("属性Key", true))
                            .build()
                    } else {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text(entry!!.key))
                            .build()
                    }
                )
                .build()
            n++

            // Type 列
            cellIds += "${name}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_entries_cell_${n}_type"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_${n}_type")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("类型", true))
                            .build()
                    } else {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text(entry!!.type.name))
                            .build()
                    }
                )
                .build()
            n++

            // Writable 列
            cellIds += "${name}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_entries_cell_${n}_writable"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_${n}_writable")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("可写", true))
                            .build()
                    } else {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text(if (entry!!.writable) "是" else "否"))
                            .build()
                    }
                )
                .build()
            n++

            // Description 列
            cellIds += "${name}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_entries_cell_${n}_description"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_${n}_description")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("描述", true))
                            .build()
                    } else {
                        Text.newBuilder()
                            .elements(text(entry!!.description))
                            .build()
                    }
                )
                .build()
            n++

            return cellIds
        }

        if (entries.size > 0) {
            ids += "${name}_entries_table"
            list += Block.newBuilder()
                .blockId("${name}_entries_table")
                .blockType(BlockBlockTypeEnum.TABLE)
                .table(
                    Table.newBuilder()
                        .property(
                            TableProperty.newBuilder()
                                .headerRow(true)
                                .columnSize(4)
                                .rowSize(entries.size + 1)
                                .columnWidth(arrayOf(150, 150, 60, 400))
                                .build()
                        )
                        .build()
                )
                .children((entriesCell(null, true) + entries.flatMap { entriesCell(it, false) }).toTypedArray())
                .build()
        }

        // 描述
        if (description.isNotBlank()) {
            ids += "${name}_quote_1"
            list += Block.newBuilder()
                .blockId("${name}_quote_1")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.QUOTE)
                .quote(Text.newBuilder().elements(text(description)).build())
                .build()
        }

        return list to ids
    }
}
