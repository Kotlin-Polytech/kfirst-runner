package org.jetbrains.research.runner.util

fun String.mapLines(f: (String) -> String): String =
    lineSequence().map(f).joinToString("\n")

fun codifyString(any: Any?, indent: String): String {
    val result = "$any"
    return "```\n$result\n```".mapLines { "    " + indent + it }
}

fun loadZestClasses(packages: List<String>): Array<Class<*>> {
    val result = ArrayList<Class<*>>()
    val contextLoader = Thread.currentThread().contextClassLoader
    val classPath = com.google.common.reflect.ClassPath.from(contextLoader)
    for (pkg in packages) {
        try {
            val zestClass = classPath.getTopLevelClasses(pkg).filter { it.name.contains("ZestTests") }.map { contextLoader.loadClass(it.name) }
            result.addAll(zestClass)
        } catch (ex: ClassNotFoundException) {

        }
    }
    return result.toTypedArray()
}
