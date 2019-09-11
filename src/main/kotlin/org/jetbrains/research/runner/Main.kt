package org.jetbrains.research.runner

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.jetbrains.research.runner.data.*
import org.jetbrains.research.runner.jackson.makeMapper
import org.jetbrains.research.runner.util.ConsoleReportListener
import org.jetbrains.research.runner.util.CustomContextClassLoaderExecutor
import org.jetbrains.research.runner.util.NoExitSecurityManager
import org.jetbrains.research.runner.util.TestReportListener
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.EngineFilter.includeEngines
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.spbstu.kotlin.generate.combinators.Arbitrary
import java.io.Closeable
import java.io.File
import java.net.URL
import java.net.URLClassLoader

val TAGS = listOf(
    "Example",
    "Trivial",
    "Easy",
    "Normal",
    "Hard",
    "Impossible",
    "No tag"
)

interface Args {
    val projectDir: String
    val classpathPrefix: List<String>
    val packages: List<String>
    val authorFile: String
    val ownerFile: String
    val resultFile: String
    val timeout: Long
}

class ParserArgs(parser: ArgParser) : Args {

    override val projectDir by parser.storing("-d", help = "project dir")

    override val classpathPrefix by parser.adding(
        "-c", "--classpath-prefix",
        help = "classpath prefix",
        initialValue = mutableListOf("/target/classes/", "/target/test-classes/")
    ) { this }

    override val packages by parser.adding("-p", help = "test packages")

    override val authorFile by parser.storing("-a", help = "author file")
        .default("author.name")

    override val ownerFile by parser.storing("-o", help = "owner file")
        .default("owner.name")

    override val resultFile by parser.storing("-r", help = "result file")
        .default("results.json")

    override val timeout by parser.storing("-t", help = "timeout") { toLong() }
        .default(50L)
}

data class RunnerArgs(
    override val projectDir: String,
    override val classpathPrefix: List<String> = mutableListOf("/target/classes/", "/target/test-classes/"),
    override val packages: List<String>,
    override val authorFile: String = "author.name",
    override val ownerFile: String = "owner.name",
    override val resultFile: String = "results.json",
    override val timeout: Long = 50L
) : Args

val logger: Logger = LoggerFactory.getLogger("Main")

fun main(arguments: Array<String>) {
    KFirstRunner().run(ParserArgs(ArgParser(arguments)))
}

class KFirstRunner {
    fun run(args: Args): TestData {

        val classpathRoots =
            args.classpathPrefix
                .map { "${args.projectDir}$it" }
                .map { URL(it) }
        val packages = args.packages

        // TODO: Support for nested packages

        val author = with(File(args.authorFile)) {
            if (exists()) readText() else "None"
        }
        val owner = with(File(args.ownerFile)) {
            if (exists()) readText() else "None"
        }

        logger.info("Classpath roots: ${classpathRoots.joinToString()}")
        logger.info("Test packages: ${packages.joinToString()}")

        val securityManagerWrapper = object : Closeable {
            val oldSecurityManager = System.getSecurityManager()

            init {
                System.setSecurityManager(
                    NoExitSecurityManager(oldSecurityManager)
                )
            }

            override fun close() {
                System.setSecurityManager(oldSecurityManager)
            }
        }

        securityManagerWrapper.use {
            CustomContextClassLoaderExecutor(
                URLClassLoader(
                    classpathRoots.toTypedArray(),
                    Thread.currentThread().contextClassLoader
                )
            ).invoke {

                val request = LauncherDiscoveryRequestBuilder
                    .request()
                    .filters(
                        includeEngines("my-junit-jupiter")
                    )
                    .selectors(
                        packages.map { selectPackage(it) }
                    )
                    .configurationParameter(
                        "my.junit.jupiter.timeout",
                        "${args.timeout}"
                    )
                    .build()

                val testReport = TestReportListener()
                val consoleReport = ConsoleReportListener()

                val seed = (System.getenv("RANDOM_SEED")?.toLong() ?: Arbitrary.defaultForLong(listOf()).next())
                    .apply { Arbitrary.random.setSeed(this) }

                val testPlan = LauncherFactory
                    .create()
                    .discover(request)

                LauncherFactory
                    .create()
                    .apply {
                        registerTestExecutionListeners(testReport)
                        registerTestExecutionListeners(consoleReport)
                    }
                    .execute(testPlan)

                with(testReport) {

                    logger.info("Result: $testData")

                    val mapper = makeMapper()

                    var totalTestData = TestData()

                    for ((pkg, pkgTests) in testData.groupByClassName().filterKeys { "" != it }) {

                        val methodTests = pkgTests.groupByMethodName()

                        val testData = methodTests.map { (method, tests) ->
                            TestDatum(
                                pkg,
                                method,
                                tests.flatMap { (id, _) -> id.tags }
                                    .map { tag -> tag.name }
                                    .toSet(),
                                tests.map { (_, r) -> r.toTestResult() }
                            )
                        }.let(::TestData)

                        totalTestData += testData

                    }

                    File(args.resultFile).writer().use {
                        mapper.writerWithDefaultPrettyPrinter().writeValue(it, totalTestData)
                    }

                    return totalTestData

                }
            }
        }
    }
}
