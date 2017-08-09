package ru.spbstu.runner.util

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import ru.spbstu.runner.data.TestDataMap

class TestReportListener : TestExecutionListener {

    val testData: TestDataMap = mutableMapOf()

    override fun executionFinished(
            testIdentifier: TestIdentifier,
            testExecutionResult: TestExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult)
        testData[testIdentifier] = testExecutionResult
    }
}
