package org.jetbrains.research.runner.jackson

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jetbrains.research.runner.data.TestData
import org.jetbrains.research.runner.data.TestInput
import java.io.File

fun TreeNode.traverseToNext(): JsonParser = traverse().apply { nextToken() }
fun TreeNode.traverseToNext(codec: ObjectCodec): JsonParser = traverse(codec).apply { nextToken() }

object MapSerializer : JsonSerializer<Map<*, *>>() {
    override fun serialize(value: Map<*, *>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartArray()
        for ((k, v) in value) {
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

object MapDeserializer : StdDeserializer<Map<*, *>>(Map::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<*, *> {
        val deser = ctxt.findNonContextualValueDeserializer(TypeFactory.unknownType())

        val res = mutableMapOf<Any?, Any?>()

        val tree: TreeNode = p.codec.readTree(p)

        val entryArray = when (tree) {
            is ArrayNode -> tree
            is ObjectNode -> JsonNodeFactory.instance.arrayNode().apply { add(tree) }
            else -> throw JsonParseException(p, "Cannot deserialize Map")
        }

        for (entry in entryArray) {
            val obj = entry as ObjectNode
            val key = deser.deserialize(obj["key"].traverseToNext(), ctxt)
            val value = deser.deserialize(obj["value"].traverseToNext(), ctxt)

            res[key] = value
        }

        return res
    }
}

object ExceptionSerializer : JsonSerializer<Exception>() {
    override fun serialize(value: Exception, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("class", value::class.qualifiedName)
        gen.writeStringField("message", value.message?.take(4096))
        gen.writeEndObject()
    }
}

object ExceptionDeserializer : StdDeserializer<Exception>(Exception::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Exception {
        val obj: ObjectNode = p.codec.readTree(p)

        val className = obj["class"].textValue()
        val message = obj["message"].textValue()

        val exClass = TypeFactory.defaultInstance().findClass(className)
        val exCtor = exClass?.constructors
                ?.firstOrNull { arrayOf(String::class.java).contentEquals(it.parameterTypes) }

        return exCtor?.newInstance(message) as? Exception ?: Exception(message)
    }
}

object InputSerializer : JsonSerializer<TestInput>() {
    override fun serialize(value: TestInput, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        for ((k, v) in value.data) {
            gen.writeFieldName(k)
            serializers.defaultSerializeValue(v, gen)
        }
        gen.writeEndObject()
    }
}

object InputDeserializer : StdDeserializer<TestInput>(TestInput::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestInput {
        val deser = ctxt.findNonContextualValueDeserializer(TypeFactory.unknownType())

        val inputMap = mutableMapOf<String, Any?>()

        val inputObject: ObjectNode = p.codec.readTree(p)
        for (field in inputObject.fields()) {
            val key = field.key
            val value = deser.deserialize(field.value.traverseToNext(p.codec), ctxt)

            inputMap[key] = value
        }

        return TestInput(inputMap)
    }
}

val serializationModule: SimpleModule = SimpleModule()
        .addSerializer(Map::class.java, MapSerializer)
        .addSerializer(Exception::class.java, ExceptionSerializer)
        .addSerializer(TestInput::class.java, InputSerializer)

val deserializationModule: SimpleModule = SimpleModule()
        .addDeserializer(Map::class.java, MapDeserializer)
        .addDeserializer(Exception::class.java, ExceptionDeserializer)
        .addDeserializer(TestInput::class.java, InputDeserializer)

fun makeMapper() = ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(Jdk8Module())
    registerModule(serializationModule)
    registerModule(deserializationModule)
    configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
}

fun main(args: Array<String>) {
    val mapper = makeMapper()

    mapper.readValue(File("results.json"), TestData::class.java)
}
