package ru.spbstu.runner.util;

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier

class ConsoleReportListener : TestExecutionListener {
    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult)
        println("Ended: $testIdentifier")
        println("  With: $testExecutionResult")
    }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        super.executionStarted(testIdentifier)
        println("Started: $testIdentifier")
    }
}
