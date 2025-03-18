package org.gitee.orryx.core.wiki

import com.lark.oapi.service.docx.v1.model.Block

interface WikiBlock {

    /**
     * 创建wiki板块
     * @return [Block] and childrenId
     */
    fun createBlocks(): Pair<List<Block>, List<String>>

}