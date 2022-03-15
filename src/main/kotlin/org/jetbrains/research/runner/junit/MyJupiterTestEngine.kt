package org.jetbrains.research.runner.junit

import org.junit.jupiter.engine.config.CachingJupiterConfiguration
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration
import org.junit.jupiter.engine.config.JupiterConfiguration
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.hierarchical.*
import java.util.*
import java.util.concurrent.TimeUnit

class MyJupiterTestEngine : HierarchicalTestEngine<JupiterEngineExecutionContext>() {

    override fun getId(): String {
        return ENGINE_ID
    }

    override fun getGroupId(): Optional<String> {
        return Optional.of("org.jetbrains.research.runner.junit")
    }

    override fun getArtifactId(): Optional<String> {
        return Optional.of("my-junit-jupiter-engine")
    }

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val configuration = CachingJupiterConfiguration(
            DefaultJupiterConfiguration(discoveryRequest.configurationParameters)
        )
        val engineDescriptor = JupiterEngineDescriptor(uniqueId, configuration)
        DiscoverySelectorResolver().resolveSelectors(discoveryRequest, engineDescriptor)
        return engineDescriptor
    }

    override fun createExecutorService(request: ExecutionRequest): HierarchicalTestExecutorService {
        val configuration = getJupiterConfiguration(request)
        val timeout = configuration.getRawConfigurationParameter("my.junit.jupiter.timeout")
            .map { it.toLong() }
            .orElse(Long.MAX_VALUE)
        return SameThreadHierarchicalTestExecutorServiceWithTimeout(timeout, TimeUnit.SECONDS)
    }

    override fun createExecutionContext(request: ExecutionRequest): JupiterEngineExecutionContext {
        return JupiterEngineExecutionContext(
            request.engineExecutionListener,
            getJupiterConfiguration(request)
        )
    }

    override fun createThrowableCollectorFactory(request: ExecutionRequest): ThrowableCollector.Factory {
        val configuration = getJupiterConfiguration(request)
        val useBlacklistedExceptions = configuration
            .getRawConfigurationParameter("my.junit.jupiter.useBlacklistedExceptions")
            .map { it.toBoolean() }
            .orElse(false)
        return if (useBlacklistedExceptions)
            ThrowableCollector.Factory { OpenTest4JAwareThrowableCollector() } else
            ThrowableCollector.Factory { OpenTest4JAndJUnit4AwareThrowableCollectorWithNoBlacklistedExceptions() }
    }

    private fun getJupiterConfiguration(request: ExecutionRequest): JupiterConfiguration {
        val engineDescriptor = request.rootTestDescriptor as JupiterEngineDescriptor
        return engineDescriptor.configuration
    }

    companion object {
        internal const val ENGINE_ID = "my-junit-jupiter"
    }

}
