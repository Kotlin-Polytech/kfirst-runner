package org.jetbrains.research.runner

import org.jetbrains.research.runner.data.UnknownFailureDatum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class KFirstRunnerTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            System.setProperty(runFakeTestsPropName, "true")
        }
    }

    @Test
    fun `runner timeouts as intended`() {
        val testData = KFirstRunner().run(
            RunnerArgs(
                projectDir = Paths.get("").toUri().toString().dropLast(1),
                packages = listOf("org.jetbrains.research.runner.fake.timeout"),
                timeout = 1
            )
        )

        assertEquals(1, testData.size)
        assertEquals(1, testData.failed.size)
        assertEquals(1, testData.failed.data[0].results.size)
        assertEquals(
            UnknownFailureDatum("java.util.concurrent.TimeoutException : timeoutTest() timed out after 1 second"),
            testData.failed.data[0].results[0].failure
        )
    }

    @Test
    fun `exit() is ignored`() {
        val testData = KFirstRunner().run(
            RunnerArgs(
                projectDir = Paths.get("").toUri().toString().dropLast(1),
                packages = listOf("org.jetbrains.research.runner.fake.exit")
            )
        )

        assertEquals(1, testData.size)
        assertEquals(1, testData.failed.size)
        assertEquals(1, testData.failed.data[0].results.size)
        assertEquals(
            UnknownFailureDatum("org.jetbrains.research.runner.util.ExitException : System.exit()"),
            testData.failed.data[0].results[0].failure
        )
    }

}
