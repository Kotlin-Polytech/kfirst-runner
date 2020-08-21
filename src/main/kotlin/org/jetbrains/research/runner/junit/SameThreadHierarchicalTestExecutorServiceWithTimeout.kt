package org.junit.platform.engine.support.hierarchical

import com.google.common.util.concurrent.SimpleTimeLimiter
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.common.util.concurrent.UncheckedTimeoutException
import org.jetbrains.research.runner.junit.TestTimeoutException
import org.junit.platform.engine.TestDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.*

class SameThreadHierarchicalTestExecutorServiceWithTimeout(
    private val timeOut: Long,
    private val timeUnit: TimeUnit
) : HierarchicalTestExecutorService {

    override fun submit(testTask: HierarchicalTestExecutorService.TestTask): Future<Void> {
        val fubarExecutable = {
            testTask.execute()
            CompletableFuture.completedFuture<Void>(null)
        }

        val nodeTask = testTask as NodeTestTask<*>
        val testDescriptor = testDescriptorField.get(nodeTask) as TestDescriptor

        try {
            return if (testDescriptor.isContainer) {
                fubarExecutable()
            } else {
                stl.callWithTimeout(
                    fubarExecutable, timeOut, timeUnit
                )
            }
        } catch (e: UncheckedTimeoutException) {
            throw TestTimeoutException(
                "Execution timed out after: $timeOut $timeUnit", testDescriptor
            )
        } catch (e: TimeoutException) {
            throw TestTimeoutException(
                "Execution timed out after: $timeOut $timeUnit", testDescriptor
            )
        }
    }

    override fun close() {}

    override fun invokeAll(testTasks: List<HierarchicalTestExecutorService.TestTask>) {
        val tc = OpenTest4JAwareThrowableCollector()

        for (task in testTasks) {
            tc.execute { submit(task).get() }
        }

        if (tc.isNotEmpty) {
            throw tc.throwable
        }
    }

    companion object {
        private val stl = SimpleTimeLimiter.create(
            Executors.newCachedThreadPool(
                ThreadFactoryBuilder()
                    .setDaemon(true)
                    .build()
            )
        )

        private val testDescriptorField =
            Class.forName(
                "org.junit.platform.engine.support.hierarchical.NodeTestTask"
            ).getDeclaredField("testDescriptor")

        init {
            testDescriptorField.isAccessible = true
        }
    }

}
