@file:Suppress("OPT_IN_USAGE")

package naksha.model.request.condition

import kotlin.js.JsExport

/**
 * Logical operation.
 */
@JsExport
abstract class LOp<T, SELF: LOp<T, SELF>> : IQuery<T>
