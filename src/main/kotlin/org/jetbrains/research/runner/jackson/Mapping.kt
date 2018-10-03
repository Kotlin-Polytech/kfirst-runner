package org.jetbrains.research.runner.jackson

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jetbrains.research.runner.data.TestData
import org.jetbrains.research.runner.data.TestFailureDatum
import org.jetbrains.research.runner.data.TestInput
import java.io.File

fun TreeNode.traverseToNext(codec: ObjectCodec): JsonParser = traverse(codec).apply { nextToken() }

fun Class<*>.unwrap(): Class<*> =
        when {
            Throwable::class.java.isAssignableFrom(this) -> Throwable::class.java
            Map::class.java.isAssignableFrom(this) -> Map::class.java
            else -> this
        }

val Any?.jsonClassName: String?
    get() = if (this != null)
        this::class.java.unwrap().canonicalName
    else
        Object::class.java.canonicalName

object TestFailureSerializer : StdSerializer<TestFailureDatum>(TestFailureDatum::class.java) {
    private fun doSerialize(value: TestFailureDatum,
                            gen: JsonGenerator,
                            provider: SerializerProvider) {
        provider.defaultSerializeField("input", value.input, gen)
        provider.defaultSerializeField("@outputClass", value.output.jsonClassName, gen)
        provider.defaultSerializeField("output", value.output, gen)
        provider.defaultSerializeField("@expectedOutputClass", value.expectedOutput.jsonClassName, gen)
        provider.defaultSerializeField("expectedOutput", value.expectedOutput, gen)
        provider.defaultSerializeField("nestedException", value.nestedException, gen)
    }

    override fun serialize(value: TestFailureDatum?,
                           gen: JsonGenerator,
                           provider: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

        gen.writeStartObject()
        doSerialize(value, gen, provider)
        gen.writeEndObject()
    }

    override fun serializeWithType(value: TestFailureDatum?,
                                   gen: JsonGenerator,
                                   serializers: SerializerProvider,
                                   typeSer: TypeSerializer) {
        if (value == null) {
            gen.writeNull()
            return
        }

        val typeId = typeSer.typeId(value, TestFailureDatum::class.java, JsonToken.START_OBJECT)

        typeSer.writeTypePrefix(gen, typeId)
        doSerialize(value, gen, serializers)
        typeSer.writeTypeSuffix(gen, typeId)
    }
}

object TestFailureDeserializer : StdDeserializer<TestFailureDatum>(TestFailureDatum::class.java) {
    override fun deserialize(p: JsonParser,
                             ctxt: DeserializationContext): TestFailureDatum {
        val testInputDeserializer = ctxt.findNonContextualValueDeserializer(
                ctxt.constructType(TestInput::class.java))

        val tree: ObjectNode = p.codec.readTree(p)

        val outputClass = tree["@outputClass"].asText()
        val expectedOutputClass = tree["@expectedOutputClass"].asText()

        val outputDeserializer = ctxt.findNonContextualValueDeserializer(
                ctxt.constructType(
                        ctxt.findClass(outputClass)))
        val expectedOutputDeserializer = ctxt.findNonContextualValueDeserializer(
                ctxt.constructType(
                        ctxt.findClass(expectedOutputClass)))

        val input = testInputDeserializer.deserialize(
                tree["input"].traverseToNext(p.codec), ctxt)
        val output = outputDeserializer.deserialize(
                tree["output"].traverseToNext(p.codec), ctxt)
        val expectedOutput = expectedOutputDeserializer.deserialize(
                tree["expectedOutput"].traverseToNext(p.codec), ctxt)
        val nestedException = tree["nestedException"].asText()

        return TestFailureDatum(input as TestInput, output, expectedOutput, nestedException)
    }
}

object MapSerializer : JsonSerializer<Map<*, *>>() {
    override fun serialize(value: Map<*, *>?,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

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
    override fun deserialize(p: JsonParser,
                             ctxt: DeserializationContext): Map<*, *> {
        val unknownDeserializer = ctxt.findNonContextualValueDeserializer(
                TypeFactory.unknownType())

        val res = mutableMapOf<Any?, Any?>()

        val entryArray: ArrayNode = p.codec.readTree(p)

        for (entry in entryArray) {
            val obj = entry as ObjectNode
            val key = unknownDeserializer.deserialize(
                    obj["key"].traverseToNext(p.codec), ctxt)
            val value = unknownDeserializer.deserialize(
                    obj["value"].traverseToNext(p.codec), ctxt)

            res[key] = value
        }

        return res
    }
}

object ThrowableSerializer : JsonSerializer<Throwable>() {
    override fun serialize(value: Throwable?,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

        gen.writeStartObject()
        gen.writeStringField("class", value::class.qualifiedName)
        gen.writeStringField("message", value.message?.take(4096))
        gen.writeEndObject()
    }
}

object ThrowableDeserializer : StdDeserializer<Throwable>(Throwable::class.java) {
    override fun deserialize(p: JsonParser,
                             ctxt: DeserializationContext): Throwable {
        val obj: ObjectNode = p.codec.readTree(p)

        val className = obj["class"].textValue()
        val message = obj["message"].textValue()

        val exClass = ctxt.typeFactory.findClass(className)
        val exCtor = exClass?.constructors
                ?.firstOrNull { arrayOf(String::class.java).contentEquals(it.parameterTypes) }

        return exCtor?.newInstance(message) as? Throwable ?: Throwable(message)
    }
}

object InputSerializer : JsonSerializer<TestInput>() {
    override fun serialize(value: TestInput?,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

        gen.writeStartObject()
        for ((k, v) in value.data) {
            gen.writeFieldName(k)
            serializers.defaultSerializeValue(v, gen)
        }
        gen.writeEndObject()
    }
}

object InputDeserializer : StdDeserializer<TestInput>(TestInput::class.java) {
    override fun deserialize(p: JsonParser,
                             ctxt: DeserializationContext): TestInput {
        val unknownDeserializer = ctxt.findNonContextualValueDeserializer(
                TypeFactory.unknownType())

        val inputMap = mutableMapOf<String, Any?>()

        val inputObject: ObjectNode = p.codec.readTree(p)

        for (field in inputObject.fields()) {
            val key = field.key
            val value = unknownDeserializer.deserialize(
                    field.value.traverseToNext(p.codec), ctxt)

            inputMap[key] = value
        }

        return TestInput(inputMap)
    }
}

val serializationModule: SimpleModule = SimpleModule()
        .addSerializer(Map::class.java, MapSerializer)
        .addSerializer(Throwable::class.java, ThrowableSerializer)
        .addSerializer(TestInput::class.java, InputSerializer)
        .addSerializer(TestFailureDatum::class.java, TestFailureSerializer)

val deserializationModule: SimpleModule = SimpleModule()
        .addDeserializer(Map::class.java, MapDeserializer)
        .addDeserializer(Throwable::class.java, ThrowableDeserializer)
        .addDeserializer(TestInput::class.java, InputDeserializer)
        .addDeserializer(TestFailureDatum::class.java, TestFailureDeserializer)

fun makeMapper() = ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(Jdk8Module())
    registerModule(serializationModule)
    registerModule(deserializationModule)
    configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
}

fun main(args: Array<String>) {
    val mapper = makeMapper()

    val value = mapper.readValue(File("results.json"), TestData::class.java)

    mapper.writeValue(File("out.json"), value)
}
