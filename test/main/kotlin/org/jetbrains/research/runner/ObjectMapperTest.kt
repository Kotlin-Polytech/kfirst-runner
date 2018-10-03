package org.jetbrains.research.runner

import org.jetbrains.research.runner.data.TestInput
import org.jetbrains.research.runner.jackson.makeMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ObjectMapperTest {

    @Test
    fun `test that mapper works as intended`() {
        val mapper = makeMapper()

        assertEquals(/* language=JSON */"""[{"key":"a","value":"b"},{"key":"Hello","value":[{"key":1,"value":2}]}]""",
                mapper.writeValueAsString(mapOf("a" to "b", "Hello" to mapOf(1 to 2))))

        assertEquals(/* language=JSON */"""{"a":"b","Hello":[{"key":1,"value":2}],"@metadata":{"@aClass":"java.lang.String","@HelloClass":"java.util.Map"}}""",
                mapper.writeValueAsString(TestInput(mapOf("a" to "b", "Hello" to mapOf(1 to 2)))))

        assertEquals(/* language=JSON */"""{"class":"java.lang.IllegalArgumentException","message":"Foo"}""",
                mapper.writeValueAsString(IllegalArgumentException("Foo")))
    }
}
