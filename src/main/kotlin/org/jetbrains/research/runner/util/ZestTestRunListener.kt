package org.jetbrains.research.runner.util

import common.TestFailureException
import org.jetbrains.research.runner.data.*
import org.junit.jupiter.api.Tag
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import java.util.concurrent.TimeoutException

class ZestTestRunListener : RunListener() {
    val testDatum = mutableMapOf<String, TestDatum>()

    override fun testFinished(description: Description) {
        val pkg = description.testClass.name
        val mtd = description.displayName.split("(")[0].removeSuffix("Test")
        val tags = description.annotations.filterIsInstance<Tag>().map { it.value }.toSet()
        val resultStatus = TestResultStatus.SUCCESSFUL
        val test = TestDatum(pkg, mtd, tags, listOf(TestResult(resultStatus)))
        testDatum.merge(mtd, test) { old, new ->
            if (old.results.isNotEmpty() && old.results.any {
                    it.status in setOf(
                        TestResultStatus.FAILED,
                        TestResultStatus.ABORTED,
                        TestResultStatus.NOT_IMPLEMENTED
                    )
                }) old else new
        }
    }

    override fun testFailure(failure: Failure) {
        val pkg = failure.description.testClass.name
        val mtd = failure.description.displayName.split("(")[0].removeSuffix("Test")
        val tags = failure.description.annotations.filterIsInstance<Tag>().map { it.value }.toSet()
        var failureDatum: FailureDatum? = null
        val resultStatus = when (val ex = failure.exception) {
            is NotImplementedError -> TestResultStatus.NOT_IMPLEMENTED
            is TestFailureException -> {
                failureDatum =
                    TestFailureDatum(TestInput(ex.input), ex.output, ex.expectedOutput, "${ex.inner}".take(8096))
                TestResultStatus.FAILED
            }
            is TimeoutException -> {
                failureDatum = UnknownFailureDatum(ex.toString())
                TestResultStatus.FAILED
            }
            else -> {
                failureDatum = UnknownFailureDatum(ex.toString())
                TestResultStatus.ABORTED
            }
        }
        val test = TestDatum(pkg, mtd, tags, listOf(TestResult(resultStatus, failureDatum)))
        testDatum.merge(mtd, test) { old, new ->
            if (old.isFailure) old else new
        }
    }
}