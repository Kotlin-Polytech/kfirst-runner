package common

class TestFailureException(
        val expectedOutput: Any?,
        val output: Any?,
        val input: Map<String, Any?>,
        override val message: String? = null,
        val inner: Throwable? = null) : Exception(inner)

class ModelFailureException(
        val expectedOutput: Any?,
        val output: Any?,
        val input: List<Any?>,
        override val message: String? = null,
        val inner: Throwable? = null) : Exception(inner)
