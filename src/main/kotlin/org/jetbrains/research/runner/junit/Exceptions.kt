package org.jetbrains.research.runner.junit

import org.junit.platform.engine.TestDescriptor

class TestTimeoutException(msg: String, val testDescriptor: TestDescriptor) : Exception(msg)
