package org.jetbrains.research.runner.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jetbrains.research.runner.data.TestInput

object MapSerializer: JsonSerializer<Map<*, *>>() {
    override fun serialize(value: Map<*, *>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartArray()
        for((k, v) in value) {
            gen.writeStartObject()
            gen.writeFieldName("key")
            serializers.defaultSerializeValue(k, gen)
            gen.writeFieldName("value")
            serializers.defaultSerializeValue(v, gen)
            gen.writeEndObject()
        }
        gen.writeEndArray()
    }
}

object ExceptionSerializer: JsonSerializer<Exception>() {
    override fun serialize(value: Exception, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("class", value::class.toString())
        gen.writeStringField("message", value.message)
        gen.writeEndObject()
    }
}

object InputSerializer: JsonSerializer<TestInput>() {
    override fun serialize(value: TestInput, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        for((k, v) in value.data) {
            gen.writeFieldName(k)
            serializers.defaultSerializeValue(v, gen)
        }
        gen.writeEndObject()
    }
}

val serializationModule = SimpleModule()
        .addSerializer(Map::class.java, MapSerializer)
        .addSerializer(Exception::class.java, ExceptionSerializer)
        .addSerializer(TestInput::class.java, InputSerializer)

fun makeMapper() = ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(Jdk8Module())
    registerModule(serializationModule)
    configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
}