package org.gitee.orryx.module.wiki

import com.lark.oapi.service.docx.v1.model.Block

/**
 * Wiki 区块构建接口。
 */
interface WikiBlock {

    /**
     * 创建 Wiki 板块。
     *
     * @return 区块列表与子区块 ID 列表
     */
    fun createBlocks(): Pair<List<Block>, List<String>>
}
