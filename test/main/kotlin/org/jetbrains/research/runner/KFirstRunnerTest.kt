package org.jetbrains.research.runner

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Paths
import java.time.Duration

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
        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            KFirstRunner().run(
                RunnerArgs(
                    projectDir = Paths.get("").toUri().toString().dropLast(1),
                    packages = listOf("org.jetbrains.research.runner.fake"),
                    timeout = 1
                )
            )
        }
    }

}
