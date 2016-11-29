package ru.spbstu.runner.util

/**
 * Created by akhin on 9/23/16.
 */

fun codifyString(any: Any?, indent: String): String {
    val result = "$any"
    return "```\n$result\n```".prependIndent("    " + indent)
}
