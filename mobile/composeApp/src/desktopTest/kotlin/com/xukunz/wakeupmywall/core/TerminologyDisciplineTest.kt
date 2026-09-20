package com.xukunz.wakeupmywall.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Phase 1 完成标准第 5 条的守卫：规范 §3 规定在线时不能说 "Connected via Wake-on-LAN"
 * （WOL 只负责开机，在线通道是 Agent）。这里扫全部产品源码，杜绝这句错误术语回归。
 */
class TerminologyDisciplineTest {

    private val forbidden = listOf(
        "Connected via Wake-on-LAN",
        "Connected via Wake on LAN",
    )

    @Test
    fun `source never claims a wake on lan connection`() {
        val moduleRoot = moduleDir()
        val offenders = listOf(moduleRoot.resolve("src/commonMain"), moduleRoot.resolve("src/androidMain"))
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .flatMap { file ->
                file.readLines().withIndex().flatMap { (index, line) ->
                    forbidden.filter { line.contains(it) }.map { "${file.relativeTo(moduleRoot)}:${index + 1}: $it" }
                }
            }

        assertTrue(offenders.isEmpty(), "出现被规范禁止的连接术语：\n" + offenders.joinToString("\n"))
    }

    private fun moduleDir(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        repeat(6) {
            val candidate = dir?.resolve("src/commonMain/kotlin/com/xukunz/wakeupmywall")
            if (candidate != null && candidate.isDirectory) return dir!!
            dir = dir?.parentFile
        }
        error("找不到模块目录，user.dir=${System.getProperty("user.dir")}")
    }
}
