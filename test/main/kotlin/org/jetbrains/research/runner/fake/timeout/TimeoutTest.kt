package org.jetbrains.research.runner.fake.timeout

import org.jetbrains.research.runner.runFakeTestsPropName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

class TimeoutTest {
    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun timeoutTest() {
        while (true) Thread.sleep(100)
    }
}
