package com.lonelymeko.myemail.common.serialization



import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable // 用于 .nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 自定义序列化器，用于将 kotlinx.datetime.LocalDateTime 序列化为 ISO 8601 字符串，
 * 并从 ISO 8601 字符串反序列化。
 * LocalDateTime 本身不包含时区信息。
 */
object LocalDateTimeIso8601Serializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.LocalDateTime.ISO8601", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        // LocalDateTime.toString() 默认输出类似 "2007-12-03T10:15:30" 的格式
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        // LocalDateTime.parse() 可以解析其 toString() 输出的格式
        return LocalDateTime.parse(decoder.decodeString())
    }
}

/**
 * 自定义序列化器，用于处理可空的 kotlinx.datetime.LocalDateTime?
 * 当值为 null 时，序列化为 null。
 * 当从 null 反序列化时，得到 null。
 */
object NullableLocalDateTimeSerializer : KSerializer<LocalDateTime?> {
    // 或者直接使用: override val descriptor: SerialDescriptor = LocalDateTimeIso8601Serializer.descriptor.nullable
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.datetime.LocalDateTime.ISO8601.Nullable", PrimitiveKind.STRING).nullable


    override fun serialize(encoder: Encoder, value: LocalDateTime?) {
        if (value == null) {
            encoder.encodeNull() // 标准做法是编码为 JSON null
        } else {
            // 当值非空时，委托给非空版本的序列化器
            encoder.encodeSerializableValue(LocalDateTimeIso8601Serializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): LocalDateTime? {
        // decodeNotNullMark() 检查下一个值是否为 null
        return if (decoder.decodeNotNullMark()) {
            // 如果非 null，则使用非空版本的序列化器解码
            decoder.decodeSerializableValue(LocalDateTimeIso8601Serializer)
        } else {
            // 如果是 null，则解码 null 并返回 null
            decoder.decodeNull()
            null
        }
    }
}