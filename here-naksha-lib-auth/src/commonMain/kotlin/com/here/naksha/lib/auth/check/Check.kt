@file:Suppress("OPT_IN_USAGE")

package com.here.naksha.lib.auth.check

import com.here.naksha.lib.base.P_AnyList
import kotlin.js.JsExport

/**
 * A test operation.
 */
@JsExport
abstract class Check : P_AnyList() {
    abstract fun matches(value: Any?): Boolean
}