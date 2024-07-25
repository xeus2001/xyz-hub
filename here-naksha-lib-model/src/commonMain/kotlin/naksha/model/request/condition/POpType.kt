package naksha.model.request.condition

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
sealed class POpType(val operation: String, val jsonOperation: String = operation) : OpType {

    data object EXISTS : POpType(" exists ", " ? ")
    data object STARTS_WITH : POpType(" like ")
    data object EQ : POpType("=")
    data object GT : POpType(">")
    data object GTE : POpType(">=")
    data object LT : POpType("<")
    data object LTE : POpType("<=")
    data object ANY : POpType("=")
    data object NULL : POpType(" is null ")
    data object NOT_NULL : POpType(" is not null")
    data object IN : POpType(" in ", " ?| ")
    data object CONTAINS : POpType("@>", " ? ")
}