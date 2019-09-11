package org.jetbrains.research.runner.fake.timeout

import org.jetbrains.research.runner.runFakeTestsPropName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

class TimeoutTest {
    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun timeoutTest01() {
        var i = 0
        while (true) i++
    }

    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun timeoutTest02() {
        var i = 0L
        while (true) i++
    }

    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun timeoutTest03() {
        while (true) Thread.sleep(100)
    }
}
