package ru.spbstu.runner.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import common.TestFailureException
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import java.util.*

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
sealed class FailureDatum

data class TestFailureDatum(
        val input: Map<String, Any?>,
        val output: Any?,
        val expectedOutput: Any?,
        val nestedException: String
) : FailureDatum()

data class UnknownFailureDatum(
        val nestedException: String
) : FailureDatum()

data class TestResult(
        val status: TestExecutionResult.Status,
        val failure: Optional<FailureDatum>
)

fun TestExecutionResult.toTestResult(): TestResult {
    val status = status
    val failure = throwable.map { ex ->
        if (ex is TestFailureException) {
            TestFailureDatum(
                    ex.input,
                    ex.output,
                    ex.expectedOutput,
                    "${ex.inner}")
        } else {
            UnknownFailureDatum(
                    "${ex.javaClass.name} : ${ex.message}"
            )
        }
    }
    return TestResult(status, failure)
}

@JsonIgnoreProperties("success", "failure")
data class TestDatum(
        val packageName: String,
        val methodName: String,
        val tags: Set<String>,
        val results: List<TestResult>
) {
    val isSuccess by lazy { results.isNotEmpty() && results.all { SUCCESSFUL == it.status } }
    val isFailure by lazy { results.isNotEmpty() && results.any { SUCCESSFUL != it.status } }
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

fun TestDataMap.groupByPackages() = entries
        .groupBy { (id, _) ->
            id.source.map { s ->
                when (s) {
                    is MethodSource -> {
                        s.className
                                .replaceAfterLast('.', "")
                                .dropLast(1)
                    }
                    else -> ""
                }
            }.orElse("")
        }

fun TestDataList.groupByMethods() =
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
