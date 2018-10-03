package org.jetbrains.research.runner.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import common.TestFailureException
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
sealed class FailureDatum

data class TestInput(val data: Map<String, Any?>)

data class TestFailureDatum(
        val input: TestInput,
        val output: Any?,
        val expectedOutput: Any?,
        val nestedException: String
) : FailureDatum()

data class UnknownFailureDatum(
        val nestedException: String
) : FailureDatum()

enum class TestResultStatus {
    SUCCESSFUL, ABORTED, FAILED, NOT_IMPLEMENTED
}

fun TestExecutionResult.Status.toOurStatus(): TestResultStatus = enumValueOf(toString())

data class TestResult(
        val status: TestResultStatus,
        val failure: FailureDatum? = null
)

fun TestExecutionResult.toTestResult(): TestResult {
    val status = status.toOurStatus()
    val ex: Throwable? = throwable.orElse(null)
    return when (ex) {
        is TestFailureException ->
            TestResult(
                    status,
                    TestFailureDatum(
                            TestInput(ex.input),
                            ex.output,
                            ex.expectedOutput,
                            "${ex.inner}".take(8096)
                    )
            )
        is NotImplementedError ->
            TestResult(TestResultStatus.NOT_IMPLEMENTED)
        null ->
            TestResult(status)
        else ->
            TestResult(
                    status,
                    UnknownFailureDatum(
                            "${ex.javaClass.name} : ${ex.message}".take(8096)
                    )
            )
    }
}

@JsonIgnoreProperties("success", "failure")
data class TestDatum(
        val packageName: String,
        val methodName: String,
        val tags: Set<String>,
        val results: List<TestResult>
) {
    val isSuccess by lazy { results.isNotEmpty() && results.all { TestResultStatus.SUCCESSFUL == it.status } }
    val isFailure by lazy { results.isNotEmpty() && results.any { it.status in setOf(TestResultStatus.FAILED, TestResultStatus.ABORTED) } }
}

@JsonIgnoreProperties("size", "succeeded", "failed", "notTagged")
data class TestData(val data: List<TestDatum>) : Iterable<TestDatum> by data {

    constructor() : this(emptyList())

    val size by lazy { data.size }

    val succeeded by lazy { data.filter { it.isSuccess }.let(::TestData) }
    val failed by lazy { data.filter { it.isFailure }.let(::TestData) }

    fun tagged(tag: String) =
            if ("No tag" == tag) notTagged
            else data.filter { tag in it.tags }.let(::TestData)

    val notTagged by lazy { data.filter { it.tags.isEmpty() }.let(::TestData) }

    operator fun plus(other: TestData) = TestData(data + other.data)

}

typealias TestDataMap = MutableMap<TestIdentifier, TestExecutionResult>
typealias TestDataList = List<Map.Entry<TestIdentifier, TestExecutionResult>>

fun TestDataMap.groupByClassName() = entries
        .groupBy { (id, _) ->
            id.source.map { s ->
                when (s) {
                    is MethodSource -> {
                        s.className
                    }
                    else -> ""
                }
            }.orElse("")
        }

fun TestDataList.groupByMethodName() =
        groupBy { (id, _) ->
            id.source.map { s ->
                when (s) {
                    is MethodSource -> {
                        s.methodName
                    }
                    else -> ""
                }
            }.orElse("")
        }
