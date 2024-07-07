package naksha.base.fn

import kotlin.js.JsExport

/**
 * A functional interface to a function lambda.
 */
@Suppress("OPT_IN_USAGE")
@JsExport
fun interface Fn2<Z, A1, A2> : Fn {
    fun call(a1: A1, a2: A2): Z
}