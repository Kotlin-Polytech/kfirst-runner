package ru.spbstu.runner.util

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ConsoleReportListener : TestExecutionListener {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(ConsoleReportListener::class.simpleName)
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult)
        logger.info("Ended: $testIdentifier")
        logger.info("With: $testExecutionResult")
    }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        super.executionStarted(testIdentifier)
        logger.info("Started: $testIdentifier")
    }
}
