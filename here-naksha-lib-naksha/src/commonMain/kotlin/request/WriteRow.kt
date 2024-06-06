package com.here.naksha.lib.naksha.request

import com.here.naksha.lib.base.response.Row
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Write operation (PUT/Upsert).
 * @see WriteFeature if you need more convenient mode with default conversions.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class WriteRow(
    collectionId: String,
    row: Row,
    val atomic: Boolean = false
) : RowOp(XYZ_OP_UPSERT, collectionId, row)