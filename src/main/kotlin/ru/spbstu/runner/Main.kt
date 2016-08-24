package ru.spbstu.runner

/**
 * Created by akhin on 8/15/16.
 */

import org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.engine.support.descriptor.JavaMethodSource
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import ru.spbstu.runner.util.CustomContextClassLoaderExecutor
import ru.spbstu.runner.util.GoogleApiFacade
import ru.spbstu.runner.util.TestReportListener
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.time.format.DateTimeFormatter
import java.util.*

fun main(args: Array<String>) {
    val classpathRoots = sequenceOf(args[0])
            .flatMap { sequenceOf(URL("$it/classes/"), URL("$it/test-classes/")) }
            .toList()
    val packages = args.drop(1)

    val author = File("author.results").readText()

    println("Classpath roots: ${classpathRoots.joinToString()}")
    println("Test packages: ${packages.joinToString()}")

    CustomContextClassLoaderExecutor(
            URLClassLoader(
                    classpathRoots.toTypedArray(),
                    Thread.currentThread().contextClassLoader
            )
    ).invoke {

        val request = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(
                        packages.map { selectPackage(it) }
                )
                .build()

        val testReport = TestReportListener()

        LauncherFactory
                .create()
                .apply { registerTestExecutionListeners(testReport) }
                .execute(request)

        val allTests = testReport.testData
                .entries
                .groupBy { e ->
                    e.key.source.map { s ->
                        when (s) {
                            is JavaMethodSource -> {
                                s.javaClass.`package`.name
                            }
//                            is JavaClassSource -> {
//                                s.javaClass.`package`.name
//                            }
//                            is JavaPackageSource -> {
//                                s.packageName
//                            }
                            else -> ""
                        }
                    }.orElseGet { "" }
                }

        val NO_TAGS = TestTag.create("No tags")

        val tags = listOf(
                "Example",
                "Trivial",
                "Easy",
                "Normal",
                "Hard",
                "Impossible"
        ).map { TestTag.create(it) }

        for (pkg in packages) {
            val pkgTests = allTests[pkg] ?: continue

            val succeededTests = pkgTests.filter { SUCCESSFUL == it.value.status }
            val failedTests = pkgTests.filter { SUCCESSFUL != it.value.status }

            val allTaggedTests = (tags
                    .map { tag ->
                        Pair(
                                tag,
                                pkgTests.filter { tag in it.key.tags }
                        )
                    } + Pair(NO_TAGS, pkgTests.filter { it.key.tags.isEmpty() }))
                    .toMap()

            val data = mutableListOf<Any>()

            data.add(DateTimeFormatter.ISO_INSTANT.format(Date().toInstant()))
            data.add(author)

            File("$pkg.results").writer().use { writer ->
                writer.appendln("Author: $author")
                writer.appendln()

                writer.appendln("Total: ${succeededTests.size} / ${pkgTests.size}")
                writer.appendln()

                for ((tag, taggedTests) in allTaggedTests) {
                    val succeededTests = taggedTests.filter { SUCCESSFUL == it.value.status }
                    data.add(succeededTests.size)
                    if (taggedTests.isEmpty()) continue
                    writer.appendln("${tag.name}: ${succeededTests.size} / ${taggedTests.size}")
                }
                writer.appendln()

                if (succeededTests.isNotEmpty()) {
                    writer.appendln("Succeeded:")
                    succeededTests.forEach { writer.appendln("* ${it.key.uniqueId}") }
                }
                writer.appendln()

                if (failedTests.isNotEmpty()) {
                    writer.appendln("Failed:")
                    failedTests.forEach { writer.appendln("* ${it.key.uniqueId}") }
                }
                writer.appendln()
            }

            GoogleApiFacade.createSheet(pkg)

            GoogleApiFacade.appendToSheet(pkg, data.map { it.toString() })

        }

    }
}
