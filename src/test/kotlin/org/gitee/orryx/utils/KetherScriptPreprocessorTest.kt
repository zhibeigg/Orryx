package org.gitee.orryx.utils

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class KetherScriptPreprocessorTest {

    @Test
    fun `纯注释行保留原始 LF 空行`() {
        val source = "# first\n  # second\n# third\n"

        assertEquals("\n  \n\n", KetherScriptPreprocessor.stripComments(source))
    }

    @Test
    fun `纯注释脚本会包装为空 main 函数`() {
        val source = "# first\r\n# second"

        assertEquals("def main = { \r\n }", KetherScriptPreprocessor.prepareScript(source))
    }

    @Test
    fun `移除行尾注释并保留注释前空白`() {
        val source = "tell first # trailing\ntell second#compact\n"

        assertEquals("tell first \ntell second\n", KetherScriptPreprocessor.stripComments(source))
    }

    @Test
    fun `单双引号内井号保持原样`() {
        val source = "tell \"# double\" # trailing\ntell '# single' # trailing\n"

        assertEquals("tell \"# double\" \ntell '# single' \n", KetherScriptPreprocessor.stripComments(source))
    }

    @Test
    fun `转义引号不会提前结束字符串`() {
        val source = """tell "escaped \"# double" # trailing
            |tell 'escaped \'# single' # trailing
        """.trimMargin()

        val expected = "tell \"escaped \\\"# double\" \ntell 'escaped \\'# single' "
        assertEquals(expected, KetherScriptPreprocessor.stripComments(source))
    }

    @Test
    fun `转义井号保留而偶数反斜杠后的井号开始注释`() {
        val source = """tell \#literal # trailing
            |tell \\# comment
        """.trimMargin()

        val expected = "tell \\#literal \ntell \\\\"
        assertEquals(expected, KetherScriptPreprocessor.stripComments(source))
    }

    @Test
    fun `CRLF 与 LF 均按原格式保留`() {
        val source = "# first\r\ntell \"# value\" # trailing\r\n# last\n"

        assertEquals("\r\ntell \"# value\" \r\n\n", KetherScriptPreprocessor.stripComments(source))
    }

    @Test
    fun `先去除注释再识别首个非空 def`() {
        val source = "# def fake = { wrong }\r\n \r\n\tdef main = {\r\n  tell ok # trailing\r\n}\r\n"
        val expected = "\r\n \r\n\tdef main = {\r\n  tell ok \r\n}\r\n"

        assertEquals(expected, KetherScriptPreprocessor.prepareScript(source))
    }

    @Test
    fun `非 def 内容统一包装且正文中的 def 不影响判断`() {
        val source = "tell default\ntell def"

        assertEquals("def main = { tell default\ntell def }", KetherScriptPreprocessor.prepareScript(source))
    }

    @Test
    fun `def 必须是首个完整词元`() {
        assertEquals(
            "def main = { define something }",
            KetherScriptPreprocessor.prepareScript("define something")
        )
        assertEquals(
            "\n\tdef main = { true }",
            KetherScriptPreprocessor.prepareScript("\n\tdef main = { true }")
        )
    }

    @Test
    fun `UTF8 字节入口使用同一预处理结果`() {
        val source = "# 中文注释\ntell \"值#1\" # trailing"
        val expected = KetherScriptPreprocessor.prepareScript(source).toByteArray(StandardCharsets.UTF_8)

        assertArrayEquals(expected, getBytes(source))
    }
}
