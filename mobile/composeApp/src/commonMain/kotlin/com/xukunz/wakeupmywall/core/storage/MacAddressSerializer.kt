package com.xukunz.wakeupmywall.core.storage

import com.xukunz.wakeupmywall.domain.model.MacAddress
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * `MacAddress` 是 value class，无法把 `@Serializable` 直接加在构造函数参数上，
 * 因此用显式序列化器保证落盘/上网的都是规范化后的冒号分隔大写形式。
 */
object MacAddressSerializer : KSerializer<MacAddress> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MacAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MacAddress) {
        encoder.encodeString(value.normalized)
    }

    override fun deserialize(decoder: Decoder): MacAddress =
        MacAddress.parse(decoder.decodeString())
            ?: throw SerializationException("Invalid MAC address")
}
