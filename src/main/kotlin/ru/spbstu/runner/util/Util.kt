package ru.spbstu.runner.util

/**
 * Created by akhin on 9/23/16.
 */

fun codifyString(any: Any?): String =
        if (null != any)
            any.toString()
                    .splitToSequence("\n")
                    .map { "    " + it }
                    .joinToString("\n")
        else "<null>"
