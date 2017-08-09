package org.jetbrains.research.runner.util

import org.jetbrains.research.runner.data.TestDataMap
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier

class TestReportListener : TestExecutionListener {

    val testData: TestDataMap = mutableMapOf()

    override fun executionFinished(
            testIdentifier: TestIdentifier,
            testExecutionResult: TestExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult)
        testData[testIdentifier] = testExecutionResult
    }
}
