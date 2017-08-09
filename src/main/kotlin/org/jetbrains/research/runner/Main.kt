package org.jetbrains.research.runner

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.jetbrains.research.runner.data.*
import org.jetbrains.research.runner.util.ConsoleReportListener
import org.jetbrains.research.runner.util.CustomContextClassLoaderExecutor
import org.jetbrains.research.runner.util.GoogleApiFacade
import org.jetbrains.research.runner.util.TestReportListener
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.spbstu.kotlin.generate.context.Gens
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.time.format.DateTimeFormatter
import java.util.*

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
    val packages: List<String>
    val classpathPrefix: List<String>
    val authorFile: String
    val ownerFile: String
    val resultFile: String
    val sendToGoogle: Boolean
}

class ParserArgs(parser: ArgParser) : Args {

    override val projectDir by parser.storing("-d", help = "project dir")

    override val packages by parser.adding("-p", help = "test packages")

    override val classpathPrefix by parser.adding("-c", "--classpath-prefix", help = "classpath prefix",
            initialValue = mutableListOf("target/classes/", "target/test-classes/")) { this }

    override val authorFile by parser.storing("-a", help = "author file")
            .default("author.name")

    override val ownerFile by parser.storing("-o", help = "owner file")
            .default("owner.name")

    override val resultFile by parser.storing("-r", help = "result file")
            .default("results.json")

    override val sendToGoogle by parser.flagging("-g", help = "send stats to Google Sheets")
            .default(false)

}

data class RunnerArgs(
        override val projectDir: String,
        override val packages: List<String>,
        override val classpathPrefix: List<String> = mutableListOf("target/classes/", "target/test-classes/"),
        override val authorFile: String = "author.name",
        override val ownerFile: String = "owner.name",
        override val resultFile: String = "results.json",
        override val sendToGoogle: Boolean = false) : Args

val logger: Logger = LoggerFactory.getLogger("Main")

fun main(arguments: Array<String>) = run(ParserArgs(ArgParser(arguments)))

fun run(args: Args) {

    val classpathRoots =
            args.classpathPrefix
                    .map { "${args.projectDir}/$it" }
                    .map { URL(it) }
    val packages = args.packages

    // TODO: Support for nested packages

    val author = File(args.authorFile).readText()
    val owner = File(args.ownerFile).readText()

    logger.info("Classpath roots: ${classpathRoots.joinToString()}")
    logger.info("Test packages: ${packages.joinToString()}")

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
        val consoleReport = ConsoleReportListener()

        val seed = (System.getenv("RANDOM_SEED")?.toLong() ?: Gens.random.nextLong())
                .apply { Gens.random.setSeed(this) }

        LauncherFactory
                .create()
                .apply {
                    registerTestExecutionListeners(testReport)
                    registerTestExecutionListeners(consoleReport)
                }
                .execute(request)

        with(testReport) {

            logger.info("$testData")

            val mapper = ObjectMapper().apply {
                registerModule(KotlinModule())
                registerModule(Jdk8Module())
            }

            var totalTestData = TestData()

            for ((pkg, pkgTests) in testData.groupByPackages().filterKeys { "" != it }) {

                val methodTests = pkgTests.groupByMethods()

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

                if (args.sendToGoogle) {
                    val data = mutableListOf<Any>()

                    data.add(DateTimeFormatter.ISO_INSTANT.format(Date().toInstant()))
                    data.add(author)
                    data.add(owner)

                    TAGS.mapTo(data) { testData.tagged(it).succeeded.size }

                    GoogleApiFacade.createSheet(pkg)
                    GoogleApiFacade.appendToSheet(pkg, data.map { it.toString() })
                }

            }

            File(args.resultFile).writer().use {
                mapper.writeValue(it, totalTestData)
            }

        }
    }
}
