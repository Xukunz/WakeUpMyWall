package com.xukunz.wakeupmywall.core.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Phase 1 全局约束：`ui/` 下的 Composable 不得出现字面量颜色、裸 dp、裸 sp。
 * 这是守卫测试（不是红-绿单测），靠它把"只能用 core/theme 令牌"从口头约定变成可执行的规则。
 */
class DesignTokenDisciplineTest {

    private val forbidden = listOf(
        Regex("""Color\(0x""") to "字面量颜色，请改用 core/theme 的颜色令牌",
        Regex("""(?<![\w.])\d+(\.\d+)?\.dp""") to "裸 dp 值，请改用 Spacing / AppShapes 令牌",
        Regex("""(?<![\w.])\d+(\.\d+)?\.sp""") to "裸 sp 值，请改用 AppTypography 令牌",
    )

    @Test
    fun `ui composables only use design tokens`() {
        val uiRoot = commonMainUiDir()
        val offenders = uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex().flatMap { (index, line) ->
                    forbidden.mapNotNull { (pattern, reason) ->
                        if (pattern.containsMatchIn(line)) {
                            "${file.relativeTo(uiRoot)}:${index + 1}: $reason | ${line.trim()}"
                        } else {
                            null
                        }
                    }
                }
            }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            "设计令牌纪律被破坏（共 ${offenders.size} 处）：\n" + offenders.joinToString("\n"),
        )
    }

    private fun commonMainUiDir(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(6) {
            val candidate = dir?.resolve("src/commonMain/kotlin/com/xukunz/wakeupmywall/ui")
            if (candidate != null && candidate.isDirectory) return candidate
            dir = dir?.parentFile
        }
        error("找不到 ui 源码目录，user.dir=${System.getProperty("user.dir")}")
    }
}
