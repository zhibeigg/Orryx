package org.gitee.orryx.module.wiki

import com.lark.oapi.service.docx.v1.enums.BlockBlockTypeEnum
import com.lark.oapi.service.docx.v1.enums.TextStyleAlignEnum
import com.lark.oapi.service.docx.v1.model.*
import taboolib.common.util.unsafeLazy

class Trigger(val group: TriggerGroup, val key: String, var description: String = ""): WikiBlock {

    internal val entries by unsafeLazy { mutableListOf<Entry>() }
    internal val specialKeyEntries by unsafeLazy { mutableListOf<Entry>() }

    class Entry(val type: Type, val key: String, val description: String)

    companion object {

        fun new(group: TriggerGroup, key: String): Trigger {
            return Trigger(group, key)
        }

    }

    fun addParm(type: Type, key: String, description: String): Trigger {
        entries += Entry(type, key, description)
        return this
    }

    fun addSpecialKey(type: Type, key: String, description: String): Trigger {
        specialKeyEntries += Entry(type, key, description)
        return this
    }

    fun description(description: String): Trigger {
        this.description = description
        return this
    }

    override fun createBlocks(): Pair<List<Block>, List<String>> {
        val list = mutableListOf<Block>()
        val id = mutableListOf<String>()
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
        id += "${key}_heading3"
        list += Block.newBuilder()
            .blockId("${key}_heading3")
            .children(arrayOf())
            .blockType(BlockBlockTypeEnum.HEADING3)
            .heading3(Text.newBuilder().elements(text(key)).build())
            .build()
        var n = 1
        fun entriesCell(entry: Entry?, isHead: Boolean): List<String> {
            val ids = mutableListOf<String>()
            ids += "${key}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${key}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${key}_entries_cell_${n}_type"))
                .build()
            list += Block.newBuilder()
                .blockId("${key}_entries_cell_${n}_type")
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
            ids += "${key}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${key}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${key}_entries_cell_${n}_description"))
                .build()
            list += Block.newBuilder()
                .blockId("${key}_entries_cell_${n}_description")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder().style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("Key", true))
                            .build()
                    } else {
                        Text.newBuilder().elements(text(entry!!.key)).build()
                    }
                )
                .build()
            n++
            ids += "${key}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${key}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${key}_entries_cell_${n}_description"))
                .build()
            list += Block.newBuilder()
                .blockId("${key}_entries_cell_${n}_description")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder().style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("Value", true))
                            .build()
                    } else {
                        Text.newBuilder().elements(text(entry!!.description)).build()
                    }
                )
                .build()
            n++
            return ids
        }
        if (entries.size > 0) {
            id += "${key}_entries_table"
            list += Block.newBuilder()
                .blockId("${key}_entries_table")
                .blockType(BlockBlockTypeEnum.TABLE)
                .table(
                    Table.newBuilder()
                        .property(
                            TableProperty.newBuilder()
                                .headerRow(true)
                                .columnSize(3)
                                .rowSize(entries.size + 1)
                                .columnWidth(arrayOf(200, 300, 400))
                                .build()
                        )
                        .build()
                )
                .children((entriesCell(null, true) + entries.flatMap { entriesCell(it, false) }).toTypedArray())
                .build()
            n = 1
        }
        if (description.isNotBlank()) {
            id += "${key}_quote_1"
            list += Block.newBuilder()
                .blockId("${key}_quote_1")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.QUOTE)
                .quote(Text.newBuilder().elements(text(description)).build())
                .build()
        }
        if (specialKeyEntries.isNotEmpty()) {
            var i = 1
            for (key in specialKeyEntries) {
                i ++
                id += "${key}_quote_$i"
                list += Block.newBuilder()
                    .blockId("${key}_quote_$i")
                    .children(arrayOf())
                    .blockType(BlockBlockTypeEnum.QUOTE)
                    .quote(Text.newBuilder().elements(text("特殊配置Key：(${key.key})，类型：(${key.type.name})，介绍：(${key.description})")).build())
                    .build()
            }
        }
        return list to id
    }
}