package org.gitee.orryx.module.wiki

import com.lark.oapi.service.docx.v1.enums.BlockBlockTypeEnum
import com.lark.oapi.service.docx.v1.enums.CalloutCalloutBackgroundColorEnum
import com.lark.oapi.service.docx.v1.enums.CalloutCalloutBorderColorEnum
import com.lark.oapi.service.docx.v1.enums.TextStyleAlignEnum
import com.lark.oapi.service.docx.v1.model.*

class Action(val group: String, val name: String, val key: String, val sharded: Boolean, val entries: MutableList<Entry> = mutableListOf(), var description: String = "", var example: String = ""): WikiBlock {

    class Entry(val description: String, val type: Type, val optional: Boolean, val default: String? = null, val head: String? = null)

    var result: Type = Type.NULL

    var resultDescription: String? = null

    companion object {

        fun new(group: String, name: String, key: String, sharded: Boolean = false): Action {
            return Action(group, name, key, sharded)
        }

    }

    fun addEntry(description: String, type: Type, optional: Boolean = false, default: String? = null, head: String? = null): Action {
        entries += Entry(description, type, optional, default, head)
        return this
    }

    fun addContainerEntry(description: String = "目标容器", optional: Boolean = false, default: String? = null, head: String? = "they"): Action {
        entries += Entry(description, Type.CONTAINER, optional, default, head)
        return this
    }

    fun addDest(type: Type, description: String = "结果存储Key", optional: Boolean = false, default: String? = null, head: String? = "dest"): Action {
        entries += Entry(description, type, optional, default, head)
        return this
    }

    fun addJob(type: Type = Type.STRING, description: String = "获取的玩家职业", optional: Boolean = false, default: String? = null, head: String? = "job"): Action {
        entries += Entry(description, type, optional, default, head)
        return this
    }

    fun description(description: String): Action {
        this.description = description
        return this
    }

    fun result(description: String, result: Type): Action {
        this.result = result
        this.resultDescription = description
        return this
    }

    fun example(example: String): Action {
        this.example = example
        return this
    }

    fun id(): String {
        return "$key $name"
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
        id += "${name}_heading3"
        list += Block.newBuilder()
            .blockId("${name}_heading3")
            .children(arrayOf())
            .blockType(BlockBlockTypeEnum.HEADING3)
            .heading3(Text.newBuilder().elements(text(
                if (sharded) {
                    "$name(公有语句)"
                } else {
                    "$name(私有语句)"
                }
            )).build())
            .build()
        val line = "$key " + entries.joinToString(" ") { entry ->
            val (start, end) = if (entry.optional) {
                "[" to "]"
            } else {
                "<" to ">"
            }
            var string = if (entry.type == Type.SYMBOL) {
                entry.head!!
            } else {
                "${start}${entry.type.name}"
            }
            if (entry.type != Type.SYMBOL) {
                if (entry.head != null) {
                    string = "*" + entry.head + " " + string
                }
                if (entry.default != null) {
                    string += "(${entry.default})"
                }
                string += end
            }
            string
        }
        id += "${name}_callout"
        list += Block.newBuilder()
            .blockId("${name}_callout")
            .blockType(BlockBlockTypeEnum.CALLOUT)
            .callout(
                Callout.newBuilder()
                    .emojiId("bulb")
                    .borderColor(CalloutCalloutBorderColorEnum.RED)
                    .backgroundColor(CalloutCalloutBackgroundColorEnum.LIGHTRED)
                    .build()
            )
            .children(arrayOf("${name}_callout_text"))
            .build()
        list += Block.newBuilder()
            .blockId("${name}_callout_text")
            .children(arrayOf())
            .blockType(BlockBlockTypeEnum.TEXT)
            .text(Text.newBuilder().elements(text(line)).build())
            .build()
        var n = 1
        fun entriesCell(entry: Entry?, isHead: Boolean): List<String> {
            val ids = mutableListOf<String>()
            ids += "${name}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_entries_cell_${n}_head"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_${n}_head")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("先导词", true))
                            .build()
                    } else {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text(entry!!.head ?: "无"))
                            .build()
                    }
                )
                .build()
            n++
            ids += "${name}_entries_cell_$n"
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
            ids += "${name}_entries_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_entries_cell_${n}_optional"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_entries_cell_${n}_optional")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("可选", true))
                            .build()
                    } else {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text(entry!!.optional.toString()))
                            .build()
                    }
                )
                .build()
            n++
            ids += "${name}_entries_cell_$n"
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
                        Text.newBuilder().style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("描述", true))
                            .build()
                    } else {
                        Text.newBuilder().elements(text(entry!!.description))
                            .build()
                    }
                )
                .build()
            n++
            return ids
        }
        if (entries.size > 0) {
            id += "${name}_entries_table"
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
                                .columnWidth(arrayOf(150, 200, 60, 400))
                                .build()
                        )
                        .build()
                )
                .children((entriesCell(null, true) + entries.flatMap { entriesCell(it, false) }).toTypedArray())
                .build()
        }
        n = 1
        fun resultCell(isHead: Boolean): List<String> {
            val ids = mutableListOf<String>()
            ids += "${name}_result_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_result_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_result_cell_${n}_result"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_result_cell_${n}_result")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("返回值类型", true)).build()
                    } else {
                        Text.newBuilder()
                            .style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text(result.name)).build()
                    }
                )
                .build()
            n++
            ids += "${name}_result_cell_$n"
            list += Block.newBuilder()
                .blockId("${name}_result_cell_$n")
                .blockType(BlockBlockTypeEnum.TABLECELL)
                .tableCell(TableCell.newBuilder().build())
                .children(arrayOf("${name}_result_cell_${n}_resultDescription"))
                .build()
            list += Block.newBuilder()
                .blockId("${name}_result_cell_${n}_resultDescription")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.TEXT)
                .text(
                    if (isHead) {
                        Text.newBuilder().style(TextStyle.newBuilder().align(TextStyleAlignEnum.CENTER).build())
                            .elements(text("描述", true)).build()
                    } else {
                        Text.newBuilder().elements(text(resultDescription ?: "无")).build()
                    }
                )
                .build()
            n++
            return ids
        }
        if (result != Type.NULL) {
            id += "${name}_result_table"
            list += Block.newBuilder()
                .blockId("${name}_result_table")
                .blockType(BlockBlockTypeEnum.TABLE)
                .table(Table.newBuilder()
                    .property(
                        TableProperty.newBuilder()
                            .headerRow(true)
                            .columnWidth(arrayOf(150, 400))
                            .columnSize(2)
                            .rowSize(2)
                            .build()
                    )
                    .build()
                )
                .children((resultCell(true) + resultCell(false)).toTypedArray())
                .build()
        }
        if (description.isNotBlank()) {
            id += "${name}_quote_1"
            list += Block.newBuilder()
                .blockId("${name}_quote_1")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.QUOTE)
                .quote(Text.newBuilder().elements(text(description)).build())
                .build()
        }
        if (example.isNotBlank()) {
            id += "${name}_quote_2"
            list += Block.newBuilder()
                .blockId("${name}_quote_2")
                .children(arrayOf())
                .blockType(BlockBlockTypeEnum.CODE)
                .code(Text.newBuilder().elements(text(example)).build())
                .build()
        }
        return list to id
    }

}