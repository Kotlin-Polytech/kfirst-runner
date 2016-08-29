package ru.spbstu.runner.data

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL

/**
 * Created by akhin on 8/29/16.
 */

data class TestDatum(
        val packageName: String,
        val methodName: String,
        val tags: Set<String>,
        val results: List<TestExecutionResult>
) {
    val isSuccess by lazy { results.isNotEmpty() && results.all { SUCCESSFUL == it.status } }
    val isFailure by lazy { results.isNotEmpty() && results.any { SUCCESSFUL != it.status } }

    val exceptions by lazy {
        results.map { it.throwable }
                .filter { it.isPresent }
                .map { it.get() }
    }
}

data class TestData(val data: List<TestDatum>) : Iterable<TestDatum> by data {
    val size by lazy { data.size }

    val succeeded by lazy { data.filter { it.isSuccess }.let { TestData(it) } }
    val failed by lazy { data.filter { it.isFailure }.let { TestData(it) } }

    fun tagged(tag: String) =
            if ("No tag" == tag) notTagged
            else data.filter { tag in it.tags }.let { TestData(it) }

    val notTagged by lazy { data.filter { it.tags.isEmpty() }.let { TestData(it) } }
}
