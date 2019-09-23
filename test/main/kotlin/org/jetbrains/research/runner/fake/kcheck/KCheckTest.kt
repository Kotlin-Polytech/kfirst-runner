package org.jetbrains.research.runner.fake.kcheck

import org.jetbrains.research.runner.runFakeTestsPropName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import ru.spbstu.kotlin.generate.combinators.KCheck.forAll

class KCheckTest {
    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun nestedForInputTest() {
        forAll { i: Int ->
            forAll { j: Int ->
                i / 0 + j / 0
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun pass() {
    }
}
