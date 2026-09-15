@file:Suppress("OPT_IN_USAGE")

package naksha.model

import naksha.base.unsupportedOp
import naksha.model.objects.NakshaCatalog
import naksha.model.objects.NakshaCollection
import naksha.model.request.FeatureTuple
import naksha.model.request.ReadRequest
import naksha.model.request.Request
import naksha.model.request.Response
import naksha.model.request.WriteRequest
import kotlin.js.JsExport

/**
 * A read session.
 * @since 3.0.0
 */
@JsExport
interface IReadSession: ISession {
    /**
     * Execute the given [Request].
     *
     * The read-only session will only be able to execute [ReadRequest]'s and throw an [naksha.base.NakshaError.UNSUPPORTED_OPERATION], when a [WriteRequest] is provided.
     * @param request the request to execute.
     * @return the response.
     * @since 2.0.7
     */
    @Deprecated(message = "Please use executeRead", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("executeRead"))
    fun execute(request: Request): Response {
        if (request is ReadRequest) return this.executeRead(request)
        if (request is WriteRequest && this is IWriteSession) return this.executeWrite(request)
        throw unsupportedOp("Unsupported request type: ${request::class.simpleName}")
    }

    /**
     * Execute the given [ReadRequest].
     * @param request the request to execute.
     * @return the response.
     * @since 2.0.7
     */
    fun executeRead(request: ReadRequest): Response

    /**
     * Returns the map for the given identifier.
     *
     * This method does only access the internal caching, and may not be up-to-date. If invoked on a [write session][IWriteSession] before committing changes, it will return catalogs that were created in the current session, but beware that these catalogs may eventually fail to commit.
     * @param catalogId the catalog-id for which to return the latest HEAD state.
     * @param allowTombstone if tombstones are returned, so _HEAD_ states indicating that the collection was deleted.
     * @return the catalog; _null_ if no such catalog exists.
     * @since 3.0
     */
    fun getCatalogById(catalogId: String, allowTombstone: Boolean = false): NakshaCatalog?

    /**
     * Returns the catalog for the given number.
     *
     * This method does only access the internal caching, and may not be up-to-date. If invoked on a [write session][IWriteSession] before committing changes, it will return catalogs that were created in the current session, but beware that these catalogs may eventually fail to commit.
     * @param catalogNumber the catalog-number for which to return the latest HEAD state.
     * @param allowTombstone if tombstones are returned, so _HEAD_ states indicating that the collection was deleted.
     * @return the catalog; _null_ if no such catalog exists.
     * @since 3.0
     */
    fun getCatalogByNumber(catalogNumber: Int, allowTombstone: Boolean = false): NakshaCatalog?

    /**
     * Returns the collection for the given identifier.
     *
     * This method does only access the internal caching, and may not be up-to-date. If invoked on a [write session][IWriteSession] before committing changes, it will return collections that were created in the current session, but beware that these collections may eventually fail to commit.
     * @param catalog the catalog to query.
     * @param collectionId the collection-id for which to return the latest HEAD state.
     * @param allowTombstone if tombstones are returned, so _HEAD_ states indicating that the collection was deleted.
     * @return the collection; _null_ if no such collection exists.
     * @since 3.0
     */
    fun getCollectionById(catalog: NakshaCatalog, collectionId: String, allowTombstone: Boolean = false): NakshaCollection?

    /**
     * Returns the collection for the given number.
     *
     * This method does only access the internal caching, and may not be up-to-date. If invoked on a [write session][IWriteSession] before committing changes, it will return collections that were created in the current session, but beware that these collections may eventually fail to commit.
     * @param catalog the catalog to query.
     * @param collectionNumber the collection-number for which to return the latest HEAD state.
     * @param allowTombstone if tombstones are returned, so _HEAD_ states indicating that the collection was deleted.
     * @return the collection; _null_ if no such collection exists.
     * @since 3.0
     */
    fun getCollectionByNumber(catalog: NakshaCatalog, collectionNumber: Int, allowTombstone: Boolean = false): NakshaCollection?

    /**
     * Load all tuples into the given [feature-tuples][FeatureTuple].
     *
     * [Tuple] that can't be fetched will still be `null` after the method returns. The method should query the [Naksha.cache] before actually loading the [Tuple] from the storage _(without asking the cache to load from storage, otherwise this would be a recursion)_ .
     *
     * @param featureTuples a list of result-tuples to fetch.
     * @param from the index of the first result-tuples to fetch; default is `0`.
     * @param to the index of the first result-tuples to ignore; default is `featureTuples.size`.
     * @since 3.0
     * @see [Naksha.cache]
     * @throws naksha.base.NakshaException if any error happens or the storage does not support tuple loading, in that case the error will be [UNSUPPORTED_OPERATION][naksha.base.NakshaError.UNSUPPORTED_OPERATION].
     */
    fun loadTuples(featureTuples: List<FeatureTuple?>, from: Int = 0, to: Int = featureTuples.size)
}