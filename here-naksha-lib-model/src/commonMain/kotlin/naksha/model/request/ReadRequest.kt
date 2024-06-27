package naksha.model.request

import naksha.model.IReadRowFilter
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
abstract class ReadRequest(
    /**
     * Limit of single response.
     * The result might have `handle` to fetch more rows.
     */
    val limit: Int = DEFAULT_LIMIT,
    /**
     * @see Request.noFeature
     * default: false
     */
    noFeature: Boolean = false,
    /**
     * @see Request.noGeometry
     * default: false
     */
    noGeometry: Boolean = false,
    /**
     * @see Request.noGeoRef
     * default: false
     */
    noGeoRef: Boolean = false,
    /**
     * @see Request.noMeta
     * default: false
     */
    noMeta: Boolean = false,
    /**
     * @see Request.noTags
     * default: false
     */
    noTags: Boolean = false,
    /**
     * @see Request.resultFilter
     * default: empty
     */
    resultFilter: Array<IReadRowFilter> = emptyArray()

) : Request(noFeature, noGeometry, noGeoRef, noMeta, noTags, resultFilter) {

    companion object {
        var DEFAULT_LIMIT = 100_000
    }
}