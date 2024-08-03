@file:Suppress("OPT_IN_USAGE")

package naksha.psql

import naksha.base.*
import naksha.base.fn.Fn0
import naksha.model.*
import naksha.model.NakshaContext.NakshaContextCompanion.currentContext
import naksha.model.NakshaError.NakshaErrorCompanion.UNAUTHORIZED
import naksha.model.NakshaError.NakshaErrorCompanion.UNSUPPORTED_OPERATION
import naksha.model.Naksha.NakshaCompanion.VIRT_COLLECTIONS
import naksha.model.Naksha.NakshaCompanion.VIRT_DICTIONARIES
import naksha.model.Naksha.NakshaCompanion.VIRT_TRANSACTIONS
import naksha.model.NakshaError.NakshaErrorCompanion.MAP_NOT_FOUND
import naksha.psql.PgUtil.PgUtilCompanion.quoteIdent
import kotlin.js.JsExport

/**
 * Information about the database and connection, that need only to be queried ones per session.
 */
@JsExport
open class PgMap(
    /**
     * The reference to the storage to which the schema belongs.
     */
    override val storage: PgStorage,

    /**
     * The map-id.
     */
    override val id: String,

    /**
     * The name of the schema.
     */
    val schemaName: String
) : IMap {
    /**
     * Admin options for this schema aka map.
     */
    fun adminOptions(): SessionOptions = storage.adminOptions.copy(mapId = id)

    internal var _number: Int? = null

    /**
     * The map-number of this map.
     */
    override val number: Int
        get() = _number ?: throw NakshaException(MAP_NOT_FOUND, "The map '$id' does not exist")

    /**
     * The lock internally used to synchronize access.
     */
    internal val lock = Platform.newLock()

    /**
     * The epoch time in milliseconds when the cache should be updated next, _null_ when no update where ever done.
     */
    private var _updateAt: Int64? = null

    /**
     * The schema
     */
    internal var _oid: Int? = null

    /**
     * The [OID](https://www.postgresql.org/docs/current/datatype-oid.html) (Object Identifier) of the schema.
     * @throws IllegalStateException if no such schema exists yet.
     */
    open val oid: Int
        get() {
            if (_updateAt == null) refresh()
            val _oid = this._oid
            check(_oid != null) { "The schema '$schemaName' does not exist" }
            return _oid
        }

    /**
     * Test if this is the default schema.
     */
    fun isDefault(): Boolean = schemaName == storage.defaultSchemaName

    /**
     * The quoted schema, for example `"foo"`, if no quoting is necessary, the string may be unquoted.
     */
    open val nameQuoted = quoteIdent(schemaName)

    /**
     * A concurrent hash map with all managed collections of this schema.
     */
    internal val collections: AtomicMap<String, WeakRef<out PgCollection>> = Platform.newAtomicMap()
    internal val collectionIdByNumber: AtomicMap<Int64, String> = Platform.newAtomicMap()

    /**
     * Returns the dictionaries' collection.
     * @return the dictionaries' collection.
     */
    open fun dictionaries(): PgNakshaDictionaries = getCollection(VIRT_DICTIONARIES) { PgNakshaDictionaries(this) }

    /**
     * Returns the transactions' collection.
     * @return the transactions' collection.
     */
    open fun transactions(): PgNakshaTransactions = getCollection(VIRT_TRANSACTIONS) { PgNakshaTransactions(this) }

    /**
     * Returns the collections' collection.
     * @return the collections' collection.
     */
    open fun collections(): PgNakshaCollections = getCollection(VIRT_COLLECTIONS) { PgNakshaCollections(this) }

    /**
     * Returns a shared cached [PgCollection] wrapper. This method is internally called, when a storage or realm are initialized to create all internal collections.
     * @param collectionId the collection-id.
     * @return the shared and cached [PgCollection] wrapper.
     */
    override operator fun get(collectionId: String): PgCollection = getCollection(collectionId) {
        when (collectionId) {
            VIRT_DICTIONARIES -> PgNakshaDictionaries(this)
            VIRT_COLLECTIONS -> PgNakshaCollections(this)
            VIRT_TRANSACTIONS -> PgNakshaTransactions(this)
            else -> PgCollection(this, collectionId)
        }
    }

    override fun getCollectionId(collectionNumber: Int64): String? = collectionIdByNumber[collectionNumber]

    /**
     * Returns a shared cached [PgCollection] wrapper. This method is internally called, when a storage or realm are initialized to create all internal collections.
     * @param id the collection identifier.
     * @param constructor the constructor to the collection, if it does not exist already.
     * @return the shared and cached [PgCollection] wrapper.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : PgCollection> getCollection(id: String, constructor: Fn0<T>): T {
        val collections = this.collections
        while (true) {
            var collection: PgCollection? = null
            val existingRef = collections[id]
            if (existingRef != null) collection = existingRef.deref()
            if (collection != null) return collection as T
            collection = constructor.call()
            val collectionRef = Platform.newWeakRef(collection)
            if (existingRef != null) {
                if (collections.replace(id, existingRef, collectionRef)) return collection
                // Conflict, another thread concurrently modified the cache.
            } else {
                collections.putIfAbsent(id, collectionRef) ?: return collection
                // Conflict, there is an existing reference, another thread concurrently access the cache.
            }
        }
    }

    /**
     * Returns either the given connection, or opens a new admin connection, when the given connection is _null_.
     */
    private fun connOf(connection: PgConnection?): PgConnection = connection ?: storage.adminConnection(adminOptions()) { _, _ -> }

    /**
     * The counter-part of [connOf], if the connection is _null_, closes [conn], if [commitOnClose] is _true_, commit changes before closing. Does nothing, when the [connection] is not _null_ ([commitOnClose] is ignored in this case).
     */
    private fun closeOf(conn: PgConnection, connection: PgConnection?, commitOnClose: Boolean) {
        if (conn !== connection) {
            if (commitOnClose) conn.commit()
            conn.close()
        }
    }

    /**
     * Refresh the schema information cache.
     *
     * This is automatically called when any value is queries for the first time.
     *
     * @param connection the connection to use to query information from the database; if _null_, a new connection is used temporary.
     * @return this.
     */
    open fun refresh(connection: PgConnection? = null): PgMap {
        if (_updateAt == null || Platform.currentMillis() < _updateAt) {
            val conn = connOf(connection)
            try {
                val cursor = conn.execute("SELECT oid FROM pg_namespace WHERE nspname = $1", arrayOf(id)).fetch()
                cursor.use {
                    _oid = cursor["oid"]
                }
            } finally {
                closeOf(conn, connection, false)
            }
            updateUpdateAt()
        }
        return this
    }

    protected fun updateUpdateAt() {
        _updateAt = Platform.currentMillis() + PlatformUtil.HOUR
    }

    /**
     * Tests if the schema exists.
     * @return _true_ if the schema exists.
     */
    override fun exists(): Boolean {
        if (_updateAt == null) refresh()
        return _oid != null
    }

    /**
     * Initialize the schema, creating all necessary database tables, installing modules, ....
     *
     * The method does auto-commit, if no [connection] was given; otherwise committing must be done explicitly.
     * @param connection the connection to use to query information from the database; if _null_, a new connection is used temporary.
     */
    open fun init(connection: PgConnection? = null) {
        // Note: Implemented in PsqlSchema!
        throw NakshaException(UNSUPPORTED_OPERATION, "This environment does not allow to initialize the schema")
    }

    /**
     * Internally called to initialize the storage.
     * @param storageId the storage-id to install, if _null_, a new storage identifier is generated.
     * @param connection the connection to use, if _null_, a new connection is created.
     * @param version the version of the PLV8 code and PSQL library, if the existing installed version is smaller, it will be updated.
     * @param override if _true_, forcefully override currently installed stored functions and PLV8 modules, even if version matches.
     * @return the storage-id given or the generated storage-id.
     */
    internal open fun init_internal(
        storageId: String?,
        connection: PgConnection,
        version: NakshaVersion = NakshaVersion.latest,
        override: Boolean = false
    ): String {
        throw NakshaException(UNSUPPORTED_OPERATION, "This environment does not allow to initialize the schema")
    }

    /**
     * Drop the schema.
     *
     * The method does auto-commit, if no [connection] was given; otherwise committing must be done explicitly.
     * @param connection the connection to use to query information from the database; if _null_, a new connection is used temporary.
     */
    open fun drop(connection: PgConnection? = null) {
        check(currentContext().su) { throw NakshaException(UNAUTHORIZED, "Only superusers may drop schemata") }
        val conn = connOf(connection)
        try {
            conn.execute("DROP SCHEMA ${quoteIdent(id)}").close()
        } finally {
            closeOf(conn, connection, true)
        }
    }
}