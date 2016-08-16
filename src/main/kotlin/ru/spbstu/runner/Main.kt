package ru.spbstu.runner

/**
 * Created by akhin on 8/15/16.
 */

import org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.engine.support.descriptor.JavaClassSource
import org.junit.platform.engine.support.descriptor.JavaMethodSource
import org.junit.platform.engine.support.descriptor.JavaPackageSource
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import ru.spbstu.runner.util.CustomContextClassLoaderExecutor
import ru.spbstu.runner.util.TestReportListener
import java.io.File
import java.net.URL
import java.net.URLClassLoader

fun main(args: Array<String>) {
    val classpathRoots = sequenceOf(args[0])
            .flatMap { sequenceOf(URL("$it/classes/"), URL("$it/test-classes/")) }
            .toList()
    val packages = args.drop(1)

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
                            is JavaClassSource -> {
                                s.javaClass.`package`.name
                            }
                            is JavaPackageSource -> {
                                s.packageName
                            }
                            else -> ""
                        }
                    }.orElseGet { "" }
                }

        for (pkg in packages) {
            val pkgTests = allTests[pkg] ?: emptyList()

            val succeededTests = pkgTests.filter { SUCCESSFUL == it.value.status }
            val failedTests = pkgTests.filter { SUCCESSFUL != it.value.status }

            File("$pkg.results").writer().use { writer ->
                writer.appendln("Total: ${succeededTests.size} / ${pkgTests.size}")

                writer.appendln("Succeeded:")
                succeededTests.forEach { writer.appendln("* ${it.key.uniqueId}") }
                writer.appendln("Failed:")
                failedTests.forEach { writer.appendln("* ${it.key.uniqueId}") }
            }
        }

    }
}
