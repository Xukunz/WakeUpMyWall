package com.xukunz.wakeupmywall.domain.model

@JvmInline
value class MacAddress private constructor(private val value: String) {
    val normalized: String get() = value
    val bytes: ByteArray get() = value.split(":").map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        fun parse(input: String): MacAddress? {
            val cleaned = input.trim().replace('-', ':').replace(" ", "")
            val parts = cleaned.split(':')
            if (parts.size != 6) return null
            val allHexPairs = parts.all { part ->
                part.length == 2 && part.all { char -> char.digitToIntOrNull(16) != null }
            }
            if (!allHexPairs) return null
            return MacAddress(parts.joinToString(":") { it.uppercase() })
        }
    }
}
