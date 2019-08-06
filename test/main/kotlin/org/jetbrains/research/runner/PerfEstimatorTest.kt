package org.jetbrains.research.runner

import org.jetbrains.research.runner.perf.median
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.spbstu.kotlin.generate.SizeInRange
import ru.spbstu.kotlin.generate.combinators.KCheck.forAll

class PerfEstimatorTest {
    @Test
    fun `median words as intended`() {
        forAll(1000) { l: @SizeInRange(16, 64) List<Int> ->
            assertEquals(
                l.sorted()[l.size / 2],
                l.median()
            )
        }
    }
}
