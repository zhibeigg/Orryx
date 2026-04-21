package org.gitee.orryx.core.reload

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReloadAnnotationTest {

    @Nested
    inner class AnnotationProperties {
        @Test
        fun `annotation has RUNTIME retention`() {
            val retention = Reload::class.annotations
                .filterIsInstance<Retention>()
                .firstOrNull()
            assertNotNull(retention)
            assertEquals(AnnotationRetention.RUNTIME, retention!!.value)
        }

        @Test
        fun `annotation targets FUNCTION`() {
            val target = Reload::class.annotations
                .filterIsInstance<Target>()
                .firstOrNull()
            assertNotNull(target)
            assertTrue(AnnotationTarget.FUNCTION in target!!.allowedTargets)
        }

        @Test
        fun `weight property exists`() {
            val weightProp = Reload::class.members.find { it.name == "weight" }
            assertNotNull(weightProp)
        }
    }

    @Nested
    inner class ReflectionUsage {

        @Reload(10)
        fun sampleFunction() {}

        @Reload(0)
        fun zeroWeightFunction() {}

        @Reload(-5)
        fun negativeWeightFunction() {}

        @Reload(Int.MAX_VALUE)
        fun maxWeightFunction() {}

        @Test
        fun `read weight via reflection`() {
            val annotation = this::class.java
                .getMethod("sampleFunction")
                .getAnnotation(Reload::class.java)
            assertNotNull(annotation)
            assertEquals(10, annotation.weight)
        }

        @Test
        fun `zero weight`() {
            val annotation = this::class.java
                .getMethod("zeroWeightFunction")
                .getAnnotation(Reload::class.java)
            assertEquals(0, annotation.weight)
        }

        @Test
        fun `negative weight`() {
            val annotation = this::class.java
                .getMethod("negativeWeightFunction")
                .getAnnotation(Reload::class.java)
            assertEquals(-5, annotation.weight)
        }

        @Test
        fun `max weight`() {
            val annotation = this::class.java
                .getMethod("maxWeightFunction")
                .getAnnotation(Reload::class.java)
            assertEquals(Int.MAX_VALUE, annotation.weight)
        }

        @Test
        fun `unannotated method returns null`() {
            val annotation = this::class.java
                .getMethod("toString")
                .getAnnotation(Reload::class.java)
            assertNull(annotation)
        }
    }
}
