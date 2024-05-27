package com.here.naksha.lib.base.response

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
class NakshaError(
    /**
     * Error code.
     */
    val error: String,
    /**
     * Human-readable message.
     */
    val message: String,
    /**
     * ID of object related to error.
     */
    val id: String? = null,
    /**
     * Original exception.
     */
    val exception: Throwable? = null
) {
}