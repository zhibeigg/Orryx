package org.gitee.orryx.module.wiki

/**
 * Wiki/Registry 使用的领域类型。
 *
 * [id] 是机器契约中的稳定标识；[parents] 与 [children] 组成可赋值类型图。
 * [ketherFillable] 表示该类型是否可以由 Kether expression 直接产生，false 时编辑器应提示使用 raw 原始值。
 */
enum class Type(
    val id: String,
    val widget: String,
    val color: String,
    val rawType: String,
    val ketherFillable: Boolean,
    val step: Double? = null
) {

    ANY("any", "text", "#B0BEC5", "java.lang.Object", true),
    NULL("null", "text", "#CFD8DC", "null", true),
    OBJECT("object", "text", "#90A4AE", "java.lang.Object", true),
    SCALAR("scalar", "text", "#78909C", "java.lang.Object", true),
    NUMBER("number", "number", "#4FC3F7", "java.lang.Number", true, 0.1),
    BYTE("byte", "number", "#4DB6AC", "java.lang.Byte", true, 1.0),
    SHORT("short", "number", "#4DB6AC", "java.lang.Short", true, 1.0),
    INT("int", "number", "#4DB6AC", "java.lang.Integer", true, 1.0),
    LONG("long", "number", "#4DB6AC", "java.lang.Long", true, 1.0),
    FLOAT("float", "number", "#4FC3F7", "java.lang.Float", true, 0.1),
    DOUBLE("double", "number", "#4FC3F7", "java.lang.Double", true, 0.1),
    STRING("text", "text", "#FFB74D", "java.lang.String", true),
    BOOLEAN("boolean", "toggle", "#E57373", "java.lang.Boolean", true),
    SYMBOL("keyword", "select", "#9E9E9E", "kether.keyword", false),
    COLLECTION("collection", "list", "#7986CB", "java.util.Collection", true),
    ITERABLE("list", "list", "#7986CB", "java.lang.Iterable", true),
    LIST("typed-list", "list", "#7986CB", "java.util.List", true),
    MAP("map", "text", "#7986CB", "java.util.Map", true),
    CONTAINER("container", "selector", "#BA68C8", "org.gitee.orryx.core.container.Container", true),
    TARGET("target", "selector", "#BA68C8", "org.gitee.orryx.core.targets.ITarget", true),
    PLAYER("player", "selector", "#AB47BC", "org.bukkit.entity.Player", true),
    ENTITY("entity", "selector", "#8D6E63", "org.bukkit.entity.Entity", true),
    LIVING_ENTITY("living-entity", "selector", "#8D6E63", "org.bukkit.entity.LivingEntity", true),
    LOCATION("location", "location", "#AED581", "org.bukkit.Location", true),
    VECTOR("vector3", "vector3", "#81C784", "org.gitee.orryx.api.adapters.vector.IVector", true),
    MATRIX("matrix", "matrix", "#A1887F", "org.joml.Matrix3d", true),
    QUATERNION("quaternion", "vector3", "#9575CD", "org.joml.Quaterniond", true),
    ITEM_STACK("item-stack", "text", "#DCE775", "org.bukkit.inventory.ItemStack", true),
    ITEM_META("item-meta", "text", "#D4E157", "org.bukkit.inventory.meta.ItemMeta", false),
    MATERIAL("material", "select", "#CDDC39", "org.bukkit.Material", false),
    NBT("nbt", "text", "#9CCC65", "taboolib.module.nms.ItemTag", false),
    HITBOX("hitbox", "text", "#FF8A65", "org.gitee.orryx.api.collider.ICollider", true),
    STATE("state", "select", "#4DD0E1", "org.gitee.orryx.module.state.IState", true),
    EFFECT("effect", "text", "#F06292", "org.gitee.orryx.core.effect.Effect", true),
    EFFECT_SPAWNER("effect-spawner", "text", "#EC407A", "org.gitee.orryx.core.effect.EffectSpawner", true),
    PROFILE("profile", "text", "#42A5F5", "org.gitee.orryx.api.profile.IProfile", false),
    SKILL("skill", "select", "#EF5350", "org.gitee.orryx.core.skill.ISkill", false),
    SKILL_PARAMETER("skill-parameter", "text", "#EF5350", "org.gitee.orryx.core.skill.SkillParameter", false),
    JOB("job", "select", "#FFA726", "org.gitee.orryx.core.job.IJob", false),
    SKILL_GROUP("skill-group", "select", "#FFB74D", "org.gitee.orryx.core.skill.SkillGroup", false),
    KEY_BINDING("key-binding", "text", "#7E57C2", "com.germ.germplugin.api.bean.KeyBinding", false),
    EVENT("event", "text", "#26A69A", "org.bukkit.event.Event", false),
    DURATION("duration", "duration", "#FFD54F", "java.time.Duration", true),
    UUID("uuid", "text", "#90CAF9", "java.util.UUID", true);

    val parents: Set<Type>
        get() = when (this) {
            ANY -> emptySet()
            NULL, OBJECT, SCALAR -> setOf(ANY)
            NUMBER -> setOf(SCALAR)
            BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> setOf(NUMBER)
            STRING, BOOLEAN, SYMBOL, UUID, DURATION -> setOf(SCALAR)
            COLLECTION -> setOf(OBJECT)
            ITERABLE, MAP, CONTAINER -> setOf(COLLECTION)
            LIST -> setOf(ITERABLE)
            TARGET -> setOf(OBJECT)
            PLAYER -> setOf(LIVING_ENTITY)
            ENTITY -> setOf(TARGET)
            LIVING_ENTITY -> setOf(ENTITY)
            LOCATION -> setOf(TARGET)
            else -> setOf(OBJECT)
        }

    val children: Set<Type>
        get() = entries.filterTo(linkedSetOf()) { this in it.parents }

    fun isAssignableFrom(actual: Type): Boolean {
        if (this == ANY || this == actual) return true
        return actual.parents.any(::isAssignableFrom)
    }

    companion object {

        fun byId(id: String): Type? = entries.firstOrNull { it.id == id }

        /**
         * 将联合槽位压缩为最小可接受类型集。
         *
         * 若父类型已经覆盖某个子类型，则移除冗余子类型；ANY 只能单独使用，
         * 避免在声明了精确类型后又用 ANY 掩盖约束。
         */
        fun minimalAcceptedTypes(types: Iterable<Type>): Set<Type> {
            val source = types.toCollection(linkedSetOf())
            require(source.isNotEmpty()) { "Action 输入至少需要一个可接受类型" }
            require(ANY !in source || source.size == 1) { "ANY 不能与精确类型同时声明" }
            return source.filterTo(linkedSetOf()) { candidate ->
                source.none { other -> other != candidate && other.isAssignableFrom(candidate) }
            }
        }
    }
}
