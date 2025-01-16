package org.gitee.orryx.core.job

enum class LevelResult {
    /**
     * 事件被取消
     * */
    CANCELLED,
    /**
     * 成功
     * */
    SUCCESS,
    /**
     * 更改值为0
     * */
    SAME;
}