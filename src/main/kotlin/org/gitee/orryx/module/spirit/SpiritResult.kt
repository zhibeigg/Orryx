package org.gitee.orryx.module.spirit

enum class SpiritResult {
    /**
     * 事件被取消
     * */
    CANCELLED,
    /**
     * 无职业
     * */
    NO_JOB,
    /**
     * 精力值不足
     * */
    NOT_ENOUGH,
    /**
     * 成功
     * */
    SUCCESS,
    /**
     * 更改值为0
     * */
    SAME;
}