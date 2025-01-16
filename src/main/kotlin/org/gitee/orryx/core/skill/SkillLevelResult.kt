package org.gitee.orryx.core.skill

enum class SkillLevelResult {
    /**
     * 事件被取消
     * */
    CANCELLED,
    /**
     * 超过最大值
     * */
    MAX,
    /**
     * 超过最小值
     * */
    MIN,
    /**
     * 等级无变化
     * */
    SAME,
    /**
     * 成功
     * */
    SUCCESS;
}