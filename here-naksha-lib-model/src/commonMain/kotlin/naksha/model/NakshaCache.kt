@file:Suppress("OPT_IN_USAGE")

package naksha.model

import naksha.base.AtomicMap
import naksha.base.WeakRef
import kotlin.js.JsExport
import kotlin.js.JsStatic
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * All kind of global caches.
 */
@JsExport
class NakshaCache private constructor() {
    companion object PgCache_C {
        private val rowCaches = AtomicMap<String, WeakRef<NakshaRowCache>>()

        /**
         * Returns the cache for all rows of a specific storage.
         * @param storageId the identifier of the storage for which to return the [row-cache][NakshaRowCache].
         * @return the [row-cache][NakshaRowCache] for the storage.
         */
        @JvmStatic
        @JsStatic
        tailrec fun rowCache(storageId: String): NakshaRowCache {
            var ref = rowCaches[storageId]
            var cache = ref?.deref()
            if (cache != null) return cache
            cache = NakshaRowCache(storageId)
            ref = WeakRef(cache)
            if (rowCaches.putIfAbsent(storageId, ref) == null) return cache
            return rowCache(storageId)
        }
    }
}