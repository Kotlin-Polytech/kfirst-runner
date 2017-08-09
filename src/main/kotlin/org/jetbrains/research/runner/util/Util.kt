package org.jetbrains.research.runner.util

fun String.mapLines(f: (String) -> String): String =
        lineSequence().map(f).joinToString("\n")

fun codifyString(any: Any?, indent: String): String {
    val result = "$any"
    return "```\n$result\n```".mapLines { "    " + indent + it }
}
