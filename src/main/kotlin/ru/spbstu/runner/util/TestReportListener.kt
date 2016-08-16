package ru.spbstu.runner.util

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier

/**
 * Created by akhin on 8/16/16.
 */

class TestReportListener : TestExecutionListener {

    val testData = mutableMapOf<TestIdentifier, TestExecutionResult>()

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult)
        testData[testIdentifier] = testExecutionResult
    }
}
