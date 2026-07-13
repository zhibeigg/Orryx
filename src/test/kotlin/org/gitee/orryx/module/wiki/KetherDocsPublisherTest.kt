package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class KetherDocsPublisherTest {

    @Test
    fun `manifest exposes stable GitHub Pages downloads`() {
        val manifest = Json.parseToJsonElement(
            KetherDocsPublisher.buildManifest(
                pluginName = "Orryx",
                version = "2.42.112",
                commit = "abcdef",
                generatedAt = Instant.parse("2025-01-02T03:04:05Z"),
                counts = KetherDocsPublisher.RegistrationCounts(
                    actions = 10,
                    selectors = 20,
                    triggers = 30,
                    properties = 40
                )
            )
        ).jsonObject

        assertEquals("Orryx", manifest.getValue("pluginId").jsonPrimitive.content)
        assertEquals("2.42.112", manifest.getValue("version").jsonPrimitive.content)
        assertEquals("2025-01-02T03:04:05Z", manifest.getValue("generatedAt").jsonPrimitive.content)
        assertEquals("abcdef", manifest.getValue("commit").jsonPrimitive.content)
        assertEquals(
            "https://zhibeigg.github.io/Orryx/kether/latest.md",
            manifest.getValue("latest").jsonPrimitive.content
        )
        assertEquals(
            "https://zhibeigg.github.io/Orryx/kether/versions/2.42.112.md",
            manifest.getValue("versioned").jsonPrimitive.content
        )
        assertEquals(
            "https://zhibeigg.github.io/Orryx/kether/actions-schema.json",
            manifest.getValue("schema").jsonPrimitive.content
        )

        val counts = manifest.getValue("counts").jsonObject
        assertEquals(10, counts.getValue("actions").jsonPrimitive.content.toInt())
        assertEquals(20, counts.getValue("selectors").jsonPrimitive.content.toInt())
        assertEquals(30, counts.getValue("triggers").jsonPrimitive.content.toInt())
        assertEquals(40, counts.getValue("properties").jsonPrimitive.content.toInt())
    }

    @Test
    fun `version filenames are sanitized`() {
        assertEquals("2.42.112_build_1", KetherDocsPublisher.sanitizeVersion("2.42.112+build/1"))
    }
}
