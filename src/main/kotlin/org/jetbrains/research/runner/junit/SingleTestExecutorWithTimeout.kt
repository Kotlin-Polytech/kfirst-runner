package org.jetbrains.research.runner.junit

import com.google.common.util.concurrent.SimpleTimeLimiter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.common.util.concurrent.UncheckedTimeoutException
import org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestExecutionResult.*
import org.junit.platform.engine.support.hierarchical.SingleTestExecutor
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.opentest4j.TestAbortedException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SingleTestExecutorWithTimeout(
        private val timeOut: Long,
        private val timeUnit: TimeUnit) : SingleTestExecutor() {

    private var currentTestId: TestIdentifier? = null

    val fubarListener: TestExecutionListener
        get() = object : TestExecutionListener {
            override fun executionStarted(testIdentifier: TestIdentifier) {
                currentTestId = testIdentifier
            }
        }

    override fun executeSafely(executable: SingleTestExecutor.Executable): TestExecutionResult {

        val fubarExecutable = {
            try {
                executable.execute()
                successful()
            } catch (e: TestAbortedException) {
                aborted(e)
            } catch (t: Throwable) {
                rethrowIfBlacklisted(t)
                failed(t)
            }
        }

        try {
            return if (currentTestId == null || currentTestId!!.isContainer) {
                fubarExecutable()
            } else {
                stl.callWithTimeout(
                        fubarExecutable, timeOut, timeUnit, true
                )
            }
        } catch (e: UncheckedTimeoutException) {
            return aborted(TimeoutException(
                    "Execution timed out after: $timeOut $timeUnit"
            ))
        } catch (t: Throwable) {
            rethrowIfBlacklisted(t)
            return failed(t)
        }

    }

    companion object {
        private val stl = SimpleTimeLimiter(
                Executors.newCachedThreadPool(
                        ThreadFactoryBuilder()
                                .setDaemon(true)
                                .build()
                )
        )
    }

}
