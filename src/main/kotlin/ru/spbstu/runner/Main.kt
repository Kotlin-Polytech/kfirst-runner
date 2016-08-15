package ru.spbstu.runner

/**
 * Created by akhin on 8/15/16.
 */

import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import ru.spbstu.runner.util.CustomContextClassLoaderExecutor
import java.io.PrintWriter
import java.net.URL
import java.net.URLClassLoader

fun main(args: Array<String>) {
    val classpathRoots = sequenceOf(args[0])
            .flatMap { sequenceOf(URL("$it/classes/"), URL("$it/test-classes/")) }
            .toList()
    val packages = args.drop(1)

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

        val summary = SummaryGeneratingListener()

        LauncherFactory
                .create()
                .apply { registerTestExecutionListeners(summary) }
                .execute(request)

        summary.summary.printTo(PrintWriter(System.out))
        summary.summary.printFailuresTo(PrintWriter(System.out))
    }
}
