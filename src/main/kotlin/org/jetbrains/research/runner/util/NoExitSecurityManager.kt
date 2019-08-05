package org.jetbrains.research.runner.util

import java.security.Permission

class ExitException : SecurityException("System.exit()")

class NoExitSecurityManager(val delegate: SecurityManager?) : SecurityManager() {
    override fun checkPermission(perm: Permission) {}

    override fun checkPermission(perm: Permission, context: Any?) {}

    override fun checkExit(status: Int) {
        delegate?.checkExit(status)
        throw ExitException()
    }
}
