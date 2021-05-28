package org.jetbrains.research.runner.util

fun String.mapLines(f: (String) -> String): String =
    lineSequence().map(f).joinToString("\n")

fun codifyString(any: Any?, indent: String): String {
    val result = "$any"
    return "```\n$result\n```".mapLines { "    " + indent + it }
}

fun loadZestClasses(packages: List<String>): Array<Class<*>> {
    val result = ArrayList<Class<*>>()
    for (pkg in packages) {
        try {
            val zestCass = Thread.currentThread().contextClassLoader.loadClass("$pkg.ZestTests")
            result.add(zestCass)
        } catch (ex: ClassNotFoundException) {

        }
    }
    return result.toTypedArray()
}
