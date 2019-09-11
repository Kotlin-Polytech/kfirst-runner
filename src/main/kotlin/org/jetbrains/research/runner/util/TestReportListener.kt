package org.jetbrains.research.runner.util

import org.jetbrains.research.runner.data.TestDataMap
import org.jetbrains.research.runner.junit.TestTimeoutException
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier

class TestReportListener : TestExecutionListener {

    val testData: TestDataMap = mutableMapOf()

    override fun executionFinished(
        testIdentifier: TestIdentifier,
        testExecutionResult: TestExecutionResult
    ) {
        val tte = testExecutionResult.throwable.map { it as? TestTimeoutException }.orElse(null)
        if (tte != null) {

            fun handleTTE(tte: TestTimeoutException) {
                val td = tte.testDescriptor
                testData.merge(TestIdentifier.from(td), TestExecutionResult.aborted(tte)) { old, new ->
                    if (old.throwable.filter { it is TestTimeoutException }.isPresent) old else new
                }
            }

            handleTTE(tte)
            for (suppressed in tte.suppressed.filterIsInstance<TestTimeoutException>()) {
                handleTTE(suppressed)
            }

        } else {
            testData.merge(testIdentifier, testExecutionResult) { old, new ->
                if (old.throwable.filter { it is TestTimeoutException }.isPresent) old else new
            }
        }
    }
}
