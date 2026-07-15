package org.gitee.orryx.module.wiki

import taboolib.library.xseries.XEntityType
import taboolib.library.xseries.XMaterial
import taboolib.library.xseries.XPotion
import taboolib.library.xseries.XSound
import taboolib.library.xseries.particles.XParticle
import java.lang.reflect.Modifier

/**
 * Kether 输入槽可复用的有限值目录。
 *
 * 目录使用 XSeries 的跨版本名称，生成 Schema 时统一去重并稳定排序。
 * Editor 只把这些值作为选择建议，仍可保留插件自定义值或 Raw Kether 片段。
 */
private fun enumConstantNames(type: Class<*>): Sequence<String> {
    return type.declaredFields.asSequence()
        .filter { it.isEnumConstant }
        .map { it.name }
}

enum class InputValueCatalog(provider: () -> Sequence<String>) {

    ENTITY_TYPE({ enumConstantNames(XEntityType::class.java) }),
    POTION_EFFECT({ enumConstantNames(XPotion::class.java) }),
    SOUND({
        XSound::class.java.fields.asSequence()
            .filter { Modifier.isStatic(it.modifiers) && it.type == XSound::class.java }
            .map { it.name }
    }),
    MATERIAL({ enumConstantNames(XMaterial::class.java) }),
    PARTICLE({ enumConstantNames(XParticle::class.java) });

    val values: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        provider()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .sorted()
            .toList()
    }
}
