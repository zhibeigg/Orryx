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
     * 无职业
     * */
    NONE,
    /**
     * 技能点不足
     * */
    POINT,
    /**
     * checkAction不通过
     * */
    CHECK,
    /**
     * 成功
     * */
    SUCCESS;
}