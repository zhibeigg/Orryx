package org.gitee.orryx.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VariableReaderTest {

    private val reader = VariableReader()

    @Nested
    inner class ReadToFlattenTest {

        @Test
        fun `empty string returns empty list`() {
            val result = reader.readToFlatten("")
            assertTrue(result.isEmpty())
        }

        @Test
        fun `no variables returns single non-variable part`() {
            val result = reader.readToFlatten("hello world")
            assertEquals(1, result.size)
            assertEquals(VariableReader.Part("hello world", false), result[0])
        }

        @Test
        fun `single variable`() {
            val result = reader.readToFlatten("hello {{name}} world")
            assertEquals(3, result.size)
            assertEquals(VariableReader.Part("hello ", false), result[0])
            assertEquals(VariableReader.Part("name", true), result[1])
            assertEquals(VariableReader.Part(" world", false), result[2])
        }

        @Test
        fun `multiple variables`() {
            val result = reader.readToFlatten("{{a}} and {{b}}")
            assertEquals(3, result.size)
            assertEquals(VariableReader.Part("a", true), result[0])
            assertEquals(VariableReader.Part(" and ", false), result[1])
            assertEquals(VariableReader.Part("b", true), result[2])
        }

        @Test
        fun `adjacent variables`() {
            val result = reader.readToFlatten("{{a}}{{b}}")
            assertEquals(2, result.size)
            assertEquals(VariableReader.Part("a", true), result[0])
            assertEquals(VariableReader.Part("b", true), result[1])
        }

        @Test
        fun `variable at start`() {
            val result = reader.readToFlatten("{{x}} tail")
            assertEquals(2, result.size)
            assertEquals(VariableReader.Part("x", true), result[0])
            assertEquals(VariableReader.Part(" tail", false), result[1])
        }

        @Test
        fun `variable at end`() {
            val result = reader.readToFlatten("head {{x}}")
            assertEquals(2, result.size)
            assertEquals(VariableReader.Part("head ", false), result[0])
            assertEquals(VariableReader.Part("x", true), result[1])
        }

        @Test
        fun `only variable`() {
            val result = reader.readToFlatten("{{var}}")
            assertEquals(1, result.size)
            assertEquals(VariableReader.Part("var", true), result[0])
        }

        @Test
        fun `unmatched start delimiter treated as plain text`() {
            val result = reader.readToFlatten("hello {{ world")
            assertEquals(1, result.size)
            assertEquals(VariableReader.Part("hello {{ world", false), result[0])
        }

        @Test
        fun `escaped delimiter inside plain text is unescaped`() {
            val result = reader.readToFlatten("before \\{{escaped\\}} after")
            assertEquals(1, result.size)
            assertEquals(VariableReader.Part("before {{escaped}} after", false), result[0])
        }
    }

    @Nested
    inner class ReplaceNestedTest {

        @Test
        fun `simple replacement`() {
            val result = reader.replaceNested("hello {{name}}") { str, _ ->
                when (str) {
                    "name" -> "world"
                    else -> str
                }
            }
            assertEquals("hello world", result)
        }

        @Test
        fun `multiple replacements`() {
            val result = reader.replaceNested("{{a}} + {{b}}") { str, _ ->
                when (str) {
                    "a" -> "1"
                    "b" -> "2"
                    else -> str
                }
            }
            assertEquals("1 + 2", result)
        }

        @Test
        fun `nested variables resolved inside out`() {
            val result = reader.replaceNested("{{outer{{inner}}}}") { str, _ ->
                when (str) {
                    "inner" -> "resolved"
                    "outerresolved" -> "final"
                    else -> str
                }
            }
            assertEquals("final", result)
        }

        @Test
        fun `deeply nested variables`() {
            val result = reader.replaceNested("{{a{{b{{c}}}}}}") { str, _ ->
                when (str) {
                    "c" -> "C"
                    "bC" -> "BC"
                    "aBC" -> "DONE"
                    else -> str
                }
            }
            assertEquals("DONE", result)
        }

        @Test
        fun `no variables returns original`() {
            val result = reader.replaceNested("plain text") { str, _ -> str }
            assertEquals("plain text", result)
        }

        @Test
        fun `empty string returns empty`() {
            val result = reader.replaceNested("") { str, _ -> str }
            assertEquals("", result)
        }

        @Test
        fun `startPos is passed correctly`() {
            val positions = mutableListOf<Int>()
            reader.replaceNested("aa{{x}}") { str, startPos ->
                positions += startPos
                str
            }
            assertEquals(listOf(2), positions)
        }

        @Test
        fun `replacement with escaped delimiters in result`() {
            val result = reader.replaceNested("{{x}}") { _, _ -> "value" }
            assertEquals("value", result)
        }
    }

    @Nested
    inner class FormatTest {

        @Test
        fun `no escapes returns same string`() {
            assertEquals("hello world", reader.format("hello world"))
        }

        @Test
        fun `escaped start delimiter is unescaped`() {
            assertEquals("{{", reader.format("\\{{"))
        }

        @Test
        fun `escaped end delimiter is unescaped`() {
            assertEquals("}}", reader.format("\\}}"))
        }

        @Test
        fun `both escaped delimiters`() {
            assertEquals("{{text}}", reader.format("\\{{text\\}}"))
        }

        @Test
        fun `backslash not followed by delimiter is preserved`() {
            assertEquals("\\hello", reader.format("\\hello"))
        }

        @Test
        fun `backslash at end of string is preserved`() {
            assertEquals("test\\", reader.format("test\\"))
        }

        @Test
        fun `empty string`() {
            assertEquals("", reader.format(""))
        }

        @Test
        fun `multiple escaped delimiters`() {
            assertEquals("{{a}} and {{b}}", reader.format("\\{{a\\}} and \\{{b\\}}"))
        }

        @Test
        fun `mixed escaped and normal text`() {
            assertEquals("pre {{ post", reader.format("pre \\{{ post"))
        }
    }

    @Nested
    inner class IndexOfTest {

        @Test
        fun `finds delimiter at start`() {
            assertEquals(0, reader.indexOf("{{hello}}", "{{"))
        }

        @Test
        fun `finds delimiter in middle`() {
            assertEquals(5, reader.indexOf("hello{{world}}", "{{"))
        }

        @Test
        fun `returns -1 when not found`() {
            assertEquals(-1, reader.indexOf("hello world", "{{"))
        }

        @Test
        fun `skips escaped delimiter`() {
            // "\{{abc{{real}}" → \{{abc{{real}} — {{ at 1 is escaped, {{ at 6 is real
            assertEquals(6, reader.indexOf("\\{{abc{{real}}", "{{"))
        }

        @Test
        fun `returns -1 when all delimiters are escaped`() {
            assertEquals(-1, reader.indexOf("\\{{only\\{{", "{{"))
        }

        @Test
        fun `respects start parameter`() {
            // "{{first}}{{second}}" — {{ at 0, }} at 7, {{ at 9
            assertEquals(9, reader.indexOf("{{first}}{{second}}", "{{", 1))
        }

        @Test
        fun `finds end delimiter`() {
            assertEquals(7, reader.indexOf("{{hello}}", "}}"))
        }

        @Test
        fun `skips escaped end delimiter`() {
            // "{{hello\}}real}}" — }} at 8 is escaped, }} at 14 is real
            assertEquals(14, reader.indexOf("{{hello\\}}real}}", "}}"))
        }

        @Test
        fun `empty source returns -1`() {
            assertEquals(-1, reader.indexOf("", "{{"))
        }
    }

    @Nested
    inner class LastIndexOfTest {

        @Test
        fun `finds last occurrence`() {
            // "{{first}}{{second}}" — {{ at 0 and 9
            assertEquals(9, reader.lastIndexOf("{{first}}{{second}}", "{{"))
        }

        @Test
        fun `finds only occurrence`() {
            assertEquals(5, reader.lastIndexOf("hello{{world}}", "{{"))
        }

        @Test
        fun `returns -1 when not found`() {
            assertEquals(-1, reader.lastIndexOf("hello world", "{{"))
        }

        @Test
        fun `skips escaped delimiter`() {
            assertEquals(0, reader.lastIndexOf("{{hello\\{{", "{{"))
        }

        @Test
        fun `returns -1 when all delimiters are escaped`() {
            assertEquals(-1, reader.lastIndexOf("\\{{only", "{{"))
        }

        @Test
        fun `respects start parameter`() {
            assertEquals(0, reader.lastIndexOf("{{first}}{{second}}", "{{", 5))
        }

        @Test
        fun `finds at position 0`() {
            assertEquals(0, reader.lastIndexOf("{{abc}}", "{{"))
        }
    }

    @Nested
    inner class CustomDelimitersTest {

        private val bracketReader = VariableReader(start = "[", end = "]")

        @Test
        fun `readToFlatten with bracket delimiters`() {
            val result = bracketReader.readToFlatten("hello [name] world")
            assertEquals(3, result.size)
            assertEquals(VariableReader.Part("hello ", false), result[0])
            assertEquals(VariableReader.Part("name", true), result[1])
            assertEquals(VariableReader.Part(" world", false), result[2])
        }

        @Test
        fun `replaceNested with bracket delimiters`() {
            val result = bracketReader.replaceNested("value is [x]") { str, _ ->
                when (str) {
                    "x" -> "42"
                    else -> str
                }
            }
            assertEquals("value is 42", result)
        }

        @Test
        fun `nested replacement with bracket delimiters`() {
            val result = bracketReader.replaceNested("[outer[inner]]") { str, _ ->
                when (str) {
                    "inner" -> "resolved"
                    "outerresolved" -> "final"
                    else -> str
                }
            }
            assertEquals("final", result)
        }

        @Test
        fun `indexOf with bracket delimiters`() {
            assertEquals(6, bracketReader.indexOf("hello [world]", "["))
        }

        @Test
        fun `escaped bracket delimiter is skipped`() {
            assertEquals(5, bracketReader.indexOf("\\[abc[real]", "["))
        }

        @Test
        fun `format unescapes bracket delimiters`() {
            assertEquals("[text]", bracketReader.format("\\[text\\]"))
        }

        @Test
        fun `multi-char custom delimiters`() {
            val percentReader = VariableReader(start = "<%", end = "%>")
            val result = percentReader.readToFlatten("hello <%name%> world")
            assertEquals(3, result.size)
            assertEquals(VariableReader.Part("name", true), result[1])
        }

        @Test
        fun `replaceNested with multi-char custom delimiters`() {
            val percentReader = VariableReader(start = "<%", end = "%>")
            val result = percentReader.replaceNested("<%a%> + <%b%>") { str, _ ->
                when (str) {
                    "a" -> "1"
                    "b" -> "2"
                    else -> str
                }
            }
            assertEquals("1 + 2", result)
        }
    }
}
