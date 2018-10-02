package org.jetbrains.research.runner.jackson

import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.jetbrains.research.runner.jackson.NumberSerializers.*
import java.io.IOException
import java.lang.reflect.Type

/**
 * Stolen from com.fasterxml.jackson.databind.ser.std.NumberSerializers
 * (and a lot of other serializers...)
 * - Includes type info for primitive types if it is requested
 */

class CustomSerializers private constructor() {
    companion object {
        fun injectIntoModule(module: SimpleModule) {
            module.addSerializer(Int::class.java, IntegerSerializer(Int::class.java))
            module.addSerializer(java.lang.Integer::class.java, IntegerSerializer(Int::class.java))
            module.addSerializer(Integer.TYPE, IntegerSerializer(Integer.TYPE))

            module.addSerializer(Long::class.java, LongSerializer(Long::class.java))
            module.addSerializer(java.lang.Long::class.java, LongSerializer(Long::class.java))
            module.addSerializer(java.lang.Long.TYPE, LongSerializer(java.lang.Long.TYPE))

            module.addSerializer(Byte::class.java, IntLikeSerializer.instance)
            module.addSerializer(java.lang.Byte::class.java, IntLikeSerializer.instance)
            module.addSerializer(java.lang.Byte.TYPE, IntLikeSerializer.instance)

            module.addSerializer(Short::class.java, ShortSerializer.instance)
            module.addSerializer(java.lang.Short::class.java, ShortSerializer.instance)
            module.addSerializer(java.lang.Short.TYPE, ShortSerializer.instance)

            module.addSerializer(Double::class.java, DoubleSerializer(Double::class.java))
            module.addSerializer(java.lang.Double::class.java, DoubleSerializer(java.lang.Double.TYPE))
            module.addSerializer(java.lang.Double.TYPE, DoubleSerializer(java.lang.Double.TYPE))

            module.addSerializer(Float::class.java, FloatSerializer.instance)
            module.addSerializer(java.lang.Float::class.java, FloatSerializer.instance)
            module.addSerializer(java.lang.Float.TYPE, FloatSerializer.instance)

            module.addSerializer(Boolean::class.java, BooleanSerializer(false))
            module.addSerializer(java.lang.Boolean::class.java, BooleanSerializer(false))
            module.addSerializer(java.lang.Boolean.TYPE, BooleanSerializer(true))

            module.addSerializer(String::class.java, StringSerializer())
            module.addSerializer(java.lang.String::class.java, StringSerializer())
        }
    }
}

internal class NumberSerializers {
    @Suppress("PropertyName", "MemberVisibilityCanBePrivate")
    internal abstract class Base<T> : StdScalarSerializer<T>, ContextualSerializer {
        protected val _numberType: JsonParser.NumberType
        protected val _schemaType: String

        protected constructor(
                cls: Class<*>,
                _numberType: JsonParser.NumberType,
                _schemaType: String) : super(cls, false) {
            this._numberType = _numberType
            this._schemaType = _schemaType
            _isInt = ((_numberType === JsonParser.NumberType.INT)
                    || (_numberType === JsonParser.NumberType.LONG)
                    || (_numberType === JsonParser.NumberType.BIG_INTEGER))
        }

        protected val _isInt: Boolean

        override fun getSchema(provider: SerializerProvider, typeHint: Type): JsonNode {
            return createSchemaNode(_schemaType, true)
        }

        @Throws(JsonMappingException::class)
        override fun acceptJsonFormatVisitor(visitor: JsonFormatVisitorWrapper,
                                             typeHint: JavaType) {
            if (_isInt) {
                visitIntFormat(visitor, typeHint, _numberType)
            } else {
                visitFloatFormat(visitor, typeHint, _numberType)
            }
        }

        @Throws(JsonMappingException::class)
        override fun createContextual(prov: SerializerProvider,
                                      property: BeanProperty): JsonSerializer<*> {
            val format = findFormatOverrides(prov, property, handledType())
            return when (format?.shape) {
                STRING -> return ToStringSerializer.instance
                else -> this
            }
        }

        override fun serializeWithType(value: T, g: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer) {
            val typeId = typeSer.typeId(value, JsonToken.VALUE_STRING)
            typeSer.writeTypePrefix(g, typeId)
            serialize(value, g, provider)
            typeSer.writeTypeSuffix(g, typeId)
        }
    }

    @JacksonStdImpl
    internal class ShortSerializer : Base<Any>(Short::class.java, JsonParser.NumberType.INT, "number") {

        @Throws(IOException::class)
        override fun serialize(value: Any,
                               gen: JsonGenerator,
                               provider: SerializerProvider) {
            gen.writeNumber((value as Short).toShort())
        }

        companion object {
            internal val instance = ShortSerializer()
        }
    }

    @JacksonStdImpl
    internal class IntegerSerializer(type: Class<*>) : Base<Any>(type, JsonParser.NumberType.INT, "integer") {

        @Throws(IOException::class)
        override fun serialize(value: Any,
                               gen: JsonGenerator,
                               provider: SerializerProvider) {
            gen.writeNumber((value as Int).toInt())
        }
    }

    @JacksonStdImpl
    internal class IntLikeSerializer : Base<Any>(Number::class.java, JsonParser.NumberType.INT, "integer") {

        @Throws(IOException::class)
        override fun serialize(value: Any,
                               gen: JsonGenerator,
                               provider: SerializerProvider) {
            gen.writeNumber((value as Number).toInt())
        }

        companion object {
            internal val instance = IntLikeSerializer()
        }
    }

    @JacksonStdImpl
    internal class LongSerializer(cls: Class<*>) : Base<Any>(cls, JsonParser.NumberType.LONG, "number") {

        @Throws(IOException::class)
        override fun serialize(value: Any,
                               gen: JsonGenerator,
                               provider: SerializerProvider) {
            gen.writeNumber((value as Long).toLong())
        }
    }

    @JacksonStdImpl
    internal class FloatSerializer : Base<Any>(Float::class.java, JsonParser.NumberType.FLOAT, "number") {

        @Throws(IOException::class)
        override fun serialize(value: Any,
                               gen: JsonGenerator,
                               provider: SerializerProvider) {
            gen.writeNumber((value as Float).toFloat())
        }

        companion object {
            internal val instance = FloatSerializer()
        }
    }

    @JacksonStdImpl
    internal class DoubleSerializer(cls: Class<*>) : Base<Any>(cls, JsonParser.NumberType.DOUBLE, "number") {

        @Throws(IOException::class)
        override fun serialize(value: Any,
                               gen: JsonGenerator,
                               provider: SerializerProvider) {
            gen.writeNumber((value as Double).toDouble())
        }
    }
}

@JacksonStdImpl
internal class BooleanSerializer(private val _forPrimitive: Boolean)
    : StdScalarSerializer<Any>(if (_forPrimitive) java.lang.Boolean.TYPE else Boolean::class.java, false), ContextualSerializer {

    @Throws(JsonMappingException::class)
    override fun createContextual(serializers: SerializerProvider,
                                  property: BeanProperty): JsonSerializer<*> {
        val format = findFormatOverrides(serializers, property, Boolean::class.java)
        if (format != null) {
            val shape = format.shape
            if (shape.isNumeric) {
                return AsNumber(_forPrimitive)
            }
        }
        return this
    }

    @Throws(IOException::class)
    override fun serialize(value: Any, g: JsonGenerator, provider: SerializerProvider) {
        g.writeBoolean(java.lang.Boolean.TRUE == value)
    }

    @Throws(IOException::class)
    override fun serializeWithType(value: Any,
                                   g: JsonGenerator,
                                   provider: SerializerProvider,
                                   typeSer: TypeSerializer) {
        val typeId = typeSer.typeId(value, JsonToken.VALUE_STRING)
        typeSer.writeTypePrefix(g, typeId)
        g.writeBoolean(java.lang.Boolean.TRUE == value)
        typeSer.writeTypeSuffix(g, typeId)
    }

    override fun getSchema(provider: SerializerProvider, typeHint: Type): JsonNode {
        return createSchemaNode("boolean", !_forPrimitive)
    }

    @Throws(JsonMappingException::class)
    override fun acceptJsonFormatVisitor(visitor: JsonFormatVisitorWrapper, typeHint: JavaType) {
        visitor.expectBooleanFormat(typeHint)
    }

    internal class AsNumber(private val _forPrimitive: Boolean)
        : StdScalarSerializer<Any>(if (_forPrimitive) java.lang.Boolean.TYPE else Boolean::class.java, false), ContextualSerializer {

        @Throws(IOException::class)
        override fun serialize(value: Any, g: JsonGenerator, provider: SerializerProvider) {
            g.writeNumber(if (java.lang.Boolean.FALSE == value) 0 else 1)
        }

        @Throws(IOException::class)
        override fun serializeWithType(value: Any,
                                       g: JsonGenerator,
                                       provider: SerializerProvider,
                                       typeSer: TypeSerializer) {
            val typeId = typeSer.typeId(value, JsonToken.VALUE_STRING)
            typeSer.writeTypePrefix(g, typeId)
            g.writeBoolean(java.lang.Boolean.TRUE == value)
            typeSer.writeTypeSuffix(g, typeId)
        }

        @Throws(JsonMappingException::class)
        override fun acceptJsonFormatVisitor(visitor: JsonFormatVisitorWrapper, typeHint: JavaType) {
            visitIntFormat(visitor, typeHint, JsonParser.NumberType.INT)
        }

        @Throws(JsonMappingException::class)
        override fun createContextual(serializers: SerializerProvider,
                                      property: BeanProperty): JsonSerializer<*> {
            val format = findFormatOverrides(serializers, property, Boolean::class.java)
            if (format != null) {
                val shape = format.shape
                if (!shape.isNumeric) {
                    return BooleanSerializer(_forPrimitive)
                }
            }
            return this
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

@JacksonStdImpl
internal class StringSerializer : StdScalarSerializer<Any>(String::class.java, false) {

    override fun isEmpty(prov: SerializerProvider, value: Any): Boolean {
        val str = value as String
        return str.isEmpty()
    }

    @Throws(IOException::class)
    override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value as String)
    }

    @Throws(IOException::class)
    override fun serializeWithType(value: Any,
                                   gen: JsonGenerator,
                                   provider: SerializerProvider,
                                   typeSer: TypeSerializer) {
        val typeId = typeSer.typeId(value, JsonToken.VALUE_STRING)
        typeSer.writeTypePrefix(gen, typeId)
        gen.writeString(value as String)
        typeSer.writeTypeSuffix(gen, typeId)
    }

    override fun getSchema(provider: SerializerProvider, typeHint: Type): JsonNode {
        return createSchemaNode("string", true)
    }

    @Throws(JsonMappingException::class)
    override fun acceptJsonFormatVisitor(visitor: JsonFormatVisitorWrapper, typeHint: JavaType) {
        visitStringFormat(visitor, typeHint)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
