@file:Suppress("OPT_IN_USAGE")

package naksha.model.request

import naksha.model.IStorage
import naksha.model.Row
import naksha.model.objects.NakshaFeature
import naksha.model.RowId
import kotlin.js.JsExport
import kotlin.jvm.JvmField

/**
 * A result row, when a row is part of some result returned by the storage.
 *
 * Note that result-rows are not thread safe, they are helpful to cache the [NakshaFeature] instance, and when there is only an optional demand to physically read all the underlying data. So, a result-row basically means that there is a row being part of a result-set, but it might not yet have been loaded into memory.
 *
 * Assume for example, there are 500,000 rows being part of a transaction, it is most often not useful to load all of them into memory, but we need at least the meta-information about all of them, so that they are part of the result-set, then normally a process steps through the result-set and stops, when enough have been processed.
 */
@JsExport
data class ResultRow(
    /**
     * The operation that was executed.
     */
    @JvmField val op: ExecutedOp,

    /**
     * Reference to storage from which the row was received.
     */
    @JvmField val storage: IStorage,

    /**
     * The map in which the row is located.
     */
    @JvmField val map: String,

    /**
     * The collection-id of the collection in which the row is located.
     */
    @JvmField val collectionId: String,

    /**
     * The row-identifier.
     */
    @JvmField val rowId: RowId,

    /**
     * The feature-id.
     *
     * Can be _null_, when not yet fetched from the storage, use [IStorage.fetchRows].
     */
    @JvmField var featureId: String?,

    /**
     * If the row is already in the cache, the reference to the row.
     *
     * Can be _null_, when not yet fetched from the storage, use [IStorage.fetchRows] or when [op] is [PURGED][ExecutedOp.PURGED] or [RETAINED][ExecutedOp.RETAINED].
     */
    @JvmField var row: Row?
) {
    /**
     * Returns the feature-id, if it is already known.
     *
     * First reads the most reliable ID from the [row], if the row is not yet fetched, tries the [featureId] property.
     * @return the feature-id, if available.
     */
    fun id() : String? = row?.meta?.id ?: featureId

    /**
     * Convert the row into a feature, and cache the feature.
     *
     * **Beware**: If the returned feature is modified, this will as well modify the cached version in the row.
     * @return the row converted into a feature, cached in this result-row.
     */
    var feature: NakshaFeature? = null
        get() {
            if (field == null) {
                if (row == null) storage.fetchRow(this)
                if (row != null) field = row?.toNakshaFeature()
            }
            return field
        }

    /**
     * Convert the row into a feature, bypass the cache.
     *
     * @return a new copy of the row converted into a feature.
     */
    fun newFeature(): NakshaFeature? = row?.toNakshaFeature()
}