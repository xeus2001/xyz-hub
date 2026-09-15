@file:Suppress("OPT_IN_USAGE")

package naksha.model

import naksha.base.Id
import naksha.base.PlatformLock
import naksha.base.fn.Fn1
import naksha.base.fn.Fx1
import naksha.jbon.IDictReader
import naksha.model.objects.NakshaStorage
import kotlin.js.JsExport

/**
 * Any entity implementing the [IStorage] interface represents some data-sink, and comes with an implementation that grants access to the data. The storage normally is a singleton that opens many sessions in parallel.
 *
 * Storages operate on maps. All storages do have an [administrative map][Naksha.ADMIN_CATALOG_ID], which can be virtual or real, implementation dependent. In this admin-map the storage exposes and manages the custom maps it stores, the transaction-logs of the storage, and the global dictionaries needed for the [JBON](https://github.com/heremaps/naksha/blob/v3/docs/JBON.md), plus optional implementation specific information.
 *
 * All other maps are custom maps, which are isolated data sinks within the same storage (like an own database schema, an own S3 bucket, an own SQLite database, an own directory or file, aso.). Each custom map is a fully separated storage entity. Within each custom map one [virtual admin collection][Naksha.COLLECTIONS_COL_ID] is exposed, which can be used to manage the collections in the map. Some storages allow to access multiple maps from one session, others may limit a session to a single map, and will reject cross map operations with [naksha.base.NakshaError.UNSUPPORTED_OPERATION].
 *
 * The storage will cache the dictionaries to avoid that just for [Tuple] decoding a new session need to be opened, which would require object creation for every single feature being decoded, therefore every storage implements the [IDictReader] interface, which internally should be attached to a storage local cache, that is automatically kept up-to-date. The same cache can be accessed from every [session][ISession], because every [session][ISession] implements as well the [dictionary-reader interface][IDictReader].
 *
 * @since 2.0.7
 */
@JsExport
interface IStorage : IDictReader {

    /**
     * A lock to be used to synchronize access to this storage.
     *
     * Usage like:
     * ```kotlin
     * storage.lock.acquire().use {
     *   // use storage
     * }
     * ```
     * @since 3.0
     */
    val lock: PlatformLock

    /**
     * The configuration object with which this storage was initialized.
     *
     * **Warning**: Modification of the returned configuration object will not have any impact on the storage, but it can provide wrong information to other callers of the function, so this should be avoided, apart from that the configuration object is not thread safe!
     * @since 3.0
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize.
     */
    val config: NakshaStorage

    /**
     * The storage-id, must always be the same for the same physical storage.
     * @since 2.0.8
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize.
     */
    val id: Id

    /**
     * The hard-cap _(max result size)_ of the storage. No result-set every can become bigger than this amount of features.
     *
     * Setting the value is optionally support, storages may throw an [naksha.base.NakshaError.UNSUPPORTED_OPERATION] exception, when trying to modify the hard-cap, or they may only allow certain values and throw an [naksha.base.NakshaError.ILLEGAL_ARGUMENT] exception, if the value too big. Zero and negative values are changed into the maximum of whatever the storage supports, [Int.MAX_VALUE] means no hard-cap (if supported by the storage).
     *
     * Note that technically, due to binary encoding, there is normally a hard-cap at `16777216`.
     * @since 3.0
     */
    val hardCap: Int

    // TODO: fun createDatabase(databaseId: String): NakshaDatabase
    //       fun upgradeDatabase(database: NakshaDatabase)
    //       fun deleteDatabase(database: NakshaDatabase)

    /**
     * Open a new write session.
     * @param options additional options, _null_ automatically creates them from the current [NakshaContext].
     * @return the write session.
     * @since 2.0.7
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize; error [UNSUPPORTED_OPERATION][naksha.base.NakshaError.UNSUPPORTED_OPERATION] if this session type is not supported by the underlying implementation.     */
    fun newWriteSession(options: SessionOptions? = null): IWriteSession

    /**
     * Open a new write-session and execute the given lambda, ensuring that the session is closed after the lambda returns.
     * @param options the session-options.
     * @param lambda the lambda to execute in a try block, ensuring that the session is closed.
     * @return the result of the lambda.
     */
    fun <T> useWriteSession(options: SessionOptions? = null, lambda: Fn1<T, IWriteSession>): T {
        val session = newWriteSession(options)
        return session.use { lambda.call(session) }
    }

    /**
     * Open a new write-session and execute the given void lambda, ensuring that the session is closed after the lambda returns.
     * This is very similar to [useWriteSession] but it's not returning any value.
     * @param options the session-options.
     * @param lambda the void lambda to execute in a try block, ensuring that the session is closed.
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize; error [UNSUPPORTED_OPERATION][naksha.base.NakshaError.UNSUPPORTED_OPERATION] if this session type is not supported by the underlying implementation.
     */
    fun runInWriteSession(options: SessionOptions? = null, lambda: Fx1<IWriteSession>) {
        val session = newWriteSession(options)
        session.use { lambda.call(session) }
    }

    /**
     * Open a new read-only session. The [SessionOptions] can be used to guarantee, that the session relates to the master-node, if replication lags are not acceptable.
     *
     * @param options additional options, _null_ automatically creates them from the current [NakshaContext].
     * @return the read-only session.
     * @since 2.0.7
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize; error [UNSUPPORTED_OPERATION][naksha.base.NakshaError.UNSUPPORTED_OPERATION] if this session type is not supported by the underlying implementation.
     */
    fun newReadSession(options: SessionOptions? = null): IReadSession

    /**
     * Open a new read-session and execute the given lambda, ensuring that the session is closed after the lambda returns.
     * @param options the session-options.
     * @param lambda the lambda to execute in a try block, ensuring that the session is closed.
     * @return the result of the lambda.
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize; error [UNSUPPORTED_OPERATION][naksha.base.NakshaError.UNSUPPORTED_OPERATION] if this session type is not supported by the underlying implementation.
     */
    fun <T> useReadSession(options: SessionOptions? = null, lambda: Fn1<T, IReadSession>): T {
        val session = newReadSession(options)
        return session.use { lambda.call(session) }
    }

    /**
     * Open a new read-session and execute the given lambda, ensuring that the session is closed after the lambda returns.
     * This is very similar to [useReadSession] but it's not returning any value.
     * @param options the session-options.
     * @param lambda the void lambda to execute in a try block, ensuring that the session is closed.
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize; error [UNSUPPORTED_OPERATION][naksha.base.NakshaError.UNSUPPORTED_OPERATION] if this session type is not supported by the underlying implementation.
     */
    fun runInReadSession(options: SessionOptions? = null, lambda: Fx1<IReadSession>) {
        val session = newReadSession(options)
        session.use { lambda.call(session) }
    }

    /**
     * Open a new streaming session to read or write data in streaming mode.
     *
     * @param options additional options, _null_ automatically creates them from the current [NakshaContext].
     * @return the streaming session.
     * @since 3.0
     * @throws naksha.base.NakshaException with error [UNINITIALIZED][naksha.base.NakshaError.UNINITIALIZED], if the storage failed to initialize; error [UNSUPPORTED_OPERATION][naksha.base.NakshaError.UNSUPPORTED_OPERATION] if this session type is not supported by the underlying implementation.
     */
    fun newStreamSession(options: SessionOptions? = null): IStreamSession
}
