package org.gitee.orryx.core.wiki

import com.lark.oapi.service.docx.v1.enums.BlockBlockTypeEnum
import com.lark.oapi.service.docx.v1.enums.CalloutCalloutBackgroundColorEnum
import com.lark.oapi.service.docx.v1.enums.CalloutCalloutBorderColorEnum
import com.lark.oapi.service.docx.v1.enums.TextStyleAlignEnum
import com.lark.oapi.service.docx.v1.model.*
import org.gitee.orryx.core.wiki.Action.Entry

class Selector(val name: String, val keys: Array<String>, val type: SelectorType, var description: String = "", val example: MutableList<String> = mutableListOf(), val entries: MutableList<Entry> = mutableListOf()): WikiBlock {

    class Entry(val type: Type, val description: String, val default: String? = null)

    companion object {

        fun new(name: String, keys: Array<String>, type: SelectorType): Selector {
            return Selector(name, keys, type)
        }

    }

    fun description(description: String): Selector {
        this.description = description
        return this
    }

    fun addExample(example: String): Selector {
        this.example += example
        return this
    }

    fun addParm(type: Type, description: String = "", default: String? = null): Selector {
        entries.add(Entry(type, description, default))
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
        id += "${name}_heading3"
        list += Block.newBuilder()
            .blockId("${name}_heading3")
            .children(arrayOf())
            .blockType(BlockBlockTypeEnum.HEADING3)
            .heading3(Text.newBuilder().elements(text(name)).build())
            .build()
        val line = "@${keys.joinToString("/")} " + entries.joinToString(" ") { entry ->
            val (start, end) = "<" to ">"
            var string = "${start}${entry.type.name}"
            if (entry.default != null) {
                string += "(${entry.default})"
            }
            string += end
            string
        }
        fun createExample(): List<String> {
            var n = 0
            return if (example.isNotEmpty()) {
                example.map { entry ->
                    val block = Block.newBuilder()
                        .blockId("${name}_quote_example_$n")
                        .children(arrayOf())
                        .blockType(BlockBlockTypeEnum.QUOTE)
                        .quote(Text.newBuilder().elements(text(entry)).build())
                        .build()
                    list += block
                    n++
                    block.blockId
                }
            } else {
                emptyList()
            }
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
            .children(arrayOf("${name}_callout_text") + createExample())
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
                        Text.newBuilder().elements(text(entry!!.description)).build()
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
                                .columnSize(2)
                                .rowSize(entries.size + 1)
                                .columnWidth(arrayOf(200, 400))
                                .build()
                        )
                        .build()
                )
                .children((entriesCell(null, true) + entries.flatMap { entriesCell(it, false) }).toTypedArray())
                .build()
            n = 1
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
        return list to id
    }

}