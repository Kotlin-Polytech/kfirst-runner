package org.jetbrains.research.runner.perf

import ru.spbstu.kotlin.generate.combinators.KCheck
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.exp
import kotlin.math.log10
import kotlin.system.measureNanoTime

inline fun <reified T : Comparable<T>> List<T>.median(): T =
    toTypedArray().kthElement(0, lastIndex, size / 2)

fun <T : Comparable<T>> Array<T>.kthElement(from: Int, to: Int, k: Int): T {
    assertInRange(from, to, k)

    val p = partition(from, to, ThreadLocalRandom.current().nextInt(0, to - from + 1))

    return when {
        p == from + k -> get(p)
        p > from + k -> kthElement(from, p - 1, k)
        else -> kthElement(p + 1, to, from + k - p - 1)
    }
}

fun <T : Comparable<T>> Array<T>.partition(from: Int, to: Int, pos: Int): Int {
    assertInRange(from, to, pos)

    swap(from + pos, to)

    val pivot = get(to)
    var i = from

    for (j in from until to) {
        if (get(j) < pivot) {
            swap(j, i)
            i++
        }
    }

    swap(to, i)

    return i
}

inline fun Array<*>.assertInRange(from: Int, to: Int, pos: Int) {
    assert(from in indices)
    assert(to in indices)
    assert(from <= to)
    assert(pos in 0..(to - from))
}

fun <T : Comparable<T>> Array<T>.swap(i: Int, j: Int) {
    val tmp = get(i)
    set(i, get(j))
    set(j, tmp)
}

data class PerfEstimation(
    val C: Double,
    val n: Double,
    val nLow: Double,
    val nHigh: Double
) {
    override fun toString(): String = "$C * x ^ $n [$nLow, $nHigh]"
}

fun <A : Number, B : Number> estimate(data: List<Pair<A, B>>): PerfEstimation {
    val avgData = data.groupBy(Pair<A, B>::first, Pair<A, B>::second)
        .map { (x, y) -> x.toDouble() to y.map(Number::toDouble).median() }
        .map { (x, y) -> log10(x) to log10(y) }

    val slopes = mutableListOf<Double>()

    for ((i, xy) in avgData.withIndex()) {
        for ((_, ab) in avgData.withIndex().drop(i + 1)) {
            slopes += (ab.second - xy.second) / (ab.first - xy.first)
        }
    }

    slopes.sort()

    val nBest = slopes[slopes.size / 2]
    val np05 = slopes[slopes.size / 20]
    val np95 = slopes[slopes.size - 1 - slopes.size / 20]

    val intercepts = avgData.map { (x, y) -> y - x * nBest }.sorted()
    val bBest = intercepts[intercepts.size / 2]

    return PerfEstimation(
        C = exp(bBest),
        n = nBest,
        nLow = np05,
        nHigh = np95
    )
}

fun main() {
    val data = mutableListOf<Pair<Int, Long>>()

    KCheck.forAll(1000) { l: List<String> ->
        val time = measureNanoTime {
            l.sorted()
        }
        data += l.size to time
    }

    val res = estimate(data)

    println(res)
}
