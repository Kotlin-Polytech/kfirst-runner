package ru.spbstu.runner.util

class CustomContextClassLoaderExecutor(val customClassLoader: ClassLoader) {

    operator fun <T> invoke(callable: () -> T): T {
        return replaceThreadContextClassLoaderAndInvoke(customClassLoader, callable)
    }

    private fun <T> replaceThreadContextClassLoaderAndInvoke(
            customClassLoader: ClassLoader,
            callable: () -> T): T {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = customClassLoader
            return callable()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

}
