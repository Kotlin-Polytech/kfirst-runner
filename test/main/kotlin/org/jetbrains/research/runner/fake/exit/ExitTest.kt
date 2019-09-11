package org.jetbrains.research.runner.fake.exit

import org.jetbrains.research.runner.runFakeTestsPropName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import kotlin.system.exitProcess

class ExitTest {
    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun exitTest() {
        exitProcess(42)
    }

    @Test
    @EnabledIfSystemProperty(named = runFakeTestsPropName, matches = "true")
    fun pass() {}
}
