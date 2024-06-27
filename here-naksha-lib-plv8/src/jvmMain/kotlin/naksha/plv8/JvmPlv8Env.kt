package com.here.naksha.lib.plv8.naksha.plv8

import com.here.naksha.lib.plv8.JvmPgConnection
import naksha.jbon.*
import naksha.model.NakshaCollectionProxy
import naksha.model.NakshaCollectionProxy.Companion.PARTITION_COUNT_NONE
import naksha.model.request.WriteFeature
import naksha.model.request.WriteRequest
import naksha.model.response.ErrorResponse
import naksha.model.response.Response
import naksha.model.response.SuccessResponse
import naksha.plv8.NKC_TABLE
import naksha.plv8.NakshaSession
import naksha.plv8.Static
import naksha.plv8.Static.SC_DICTIONARIES
import naksha.plv8.Static.SC_INDICES
import naksha.plv8.Static.SC_TRANSACTIONS
import java.sql.Connection

/**
 * Simulates an PLV8 environment to allow better testing. Technically, except for the [install] method, this class
 * is never needed in `lib-psql` or other Java code, the Java code should always rely upon the SQL functions.
 *
 * This class extends the standard [JvmEnv] by SQL support to simulate the internal database connection of PLV8.
 */
class JvmPlv8Env(val storage: PsqlStorage) {
    // ============================================================================================================
    //                             Additional code only available in Java
    // ============================================================================================================

    /**
     * Simulates the SQL function `naksha_start_session`. Creates the [NakshaSession] can binds the [NakshaSession.sql]
     * to the given SQL connection, so that all other session methods work the same way they would normally behave
     * inside the Postgres database (so, when executed in PLV8 engine).
     * @param conn The connection to bind to the [NakshaSession].
     * @param schema The schema to use.
     * @param appName The name of this application (arbitrary string, seen in SQL connections).
     * @param streamId The logging stream-id, can be found in transaction logs for error search.
     * @param appId The UPM application identifier.
     * @param author The UPM user identifier that uses this session.
     * @return NakshaSession
     */
    fun startSession(conn: Connection, schema: String, appName: String, streamId: String, appId: String, author: String?): NakshaSession {
        // Prepare the connection, need to load the module system.
        conn.autoCommit = false
        val sql = JvmPgConnection(conn)
        val schemaQuoted = sql.quoteIdent(schema)
        sql.execute("SET SESSION search_path TO $schemaQuoted, public, topology; SELECT naksha_start_session($1, $2, $3, $4);",
            arrayOf(appName, streamId, appId, author))
        conn.commit()
        // Create our self.
        // Create the JVM Naksha session.
        val data = sql.rows(sql.execute("SELECT naksha_storage_id() as storage_id, naksha_schema() as schema"))!![0]
        return NakshaSession(sql, data["schema"]!! as String, storage , appName, streamId, appId, author)
    }

    private fun getResourceAsText(path: String): String? =
            object {}.javaClass.getResource(path)?.readText()

    private fun applyReplacements(text: String, replacements: Map<String, String>?): String {
        if (replacements != null) {
            var t = text
            val sb = StringBuilder()
            for (entry in replacements) {
                sb.setLength(0)
                sb.append('$').append('{').append(entry.key).append('}')
                t = t.replace(sb.toString(), entry.value, true)
            }
            return t
        } else {
            return text
        }
    }

    /**
     * Execute the SQL being in the file.
     * @param sql The connection to use for the installation.
     * @param path The file-path, for example `/lz4.sql`.
     * @param replacements A map of replacements (`${name}`) that should be replaced with the given value in the source.
     */
    private fun executeSqlFromResource(sql: JvmPgConnection, path: String, replacements: Map<String, String>? = null) {
        val resourceAsText = getResourceAsText(path)
        check(resourceAsText != null)
        sql.execute(applyReplacements(resourceAsText, replacements))
    }

    /**
     * Install a JS module with the given name from the given resource file.
     * @param sql The connection to use for the installation.
     * @param name The module name, for example `lz4`.
     * @param path The file-path, for example `/lz4.js`.
     * @param autoload If the module should be automatically loaded.
     * @param beautify If the source should be beautified before insertion.
     * @param extraCode Additional code to be executed, appended at the end of the module.
     * @param replacements A map of replacements (`${name}`) that should be replaced with the given value in the source.
     */
    private fun installModuleFromResource(sql: JvmPgConnection, name: String, path: String, autoload: Boolean = false, beautify: Boolean = false, extraCode: String? = null, replacements: Map<String, String>? = null) {
        val resourceAsText = getResourceAsText(path)
        check(resourceAsText != null)
        var code = applyReplacements(resourceAsText, replacements)
        if (extraCode != null) code += "\n" + extraCode
        val dollar3 = if (beautify) "js_beautify(\$3)" else "\$3"
        val query = "INSERT INTO commonjs2_modules (module, autoload, source) VALUES (\$1, \$2, $dollar3) " +
                "ON CONFLICT (module) DO UPDATE SET autoload = $2, source = $dollar3"
        sql.execute(query, arrayOf(name, autoload, code))
    }

    /**
     * Installs the `commonjs2`, `lz4`, `jbon` and `naksha` modules into a PostgresQL database with a _PLV8_ extension. Must only
     * be executed ones per storage. This as well creates the needed admin-tables (for transactions, global dictionary aso.).
     * @param conn The connection to use for the installation.
     * @param version The Naksha Version.
     */
    fun install(conn: PsqlConnection, version: Long, schema: String, storageId: String, appName: String) {
        conn.pgConnection.autoCommit = false
        val sql = JvmPgConnection(conn.pgConnection)
        val schemaQuoted = sql.quoteIdent(schema)
        sql.execute("""
CREATE SCHEMA IF NOT EXISTS $schemaQuoted;
SET SESSION search_path TO $schemaQuoted, public, topology;
""")
        val schemaOid: Int = sql.rows(sql.execute("SELECT oid FROM pg_namespace WHERE nspname = $1", arrayOf(schema)))!![0]["oid"]!! as Int

        executeSqlFromResource(sql, "/commonjs2.sql")
        installModuleFromResource(sql, "beautify", "/beautify.min.js")
        executeSqlFromResource(sql, "/beautify.sql")
        sql.execute("""DO $$
var commonjs2_init = plv8.find_function("commonjs2_init");
commonjs2_init();
$$ LANGUAGE 'plv8';""")
        installModuleFromResource(sql, "lz4_util", "/lz4_util.js")
        installModuleFromResource(sql, "lz4_xxhash", "/lz4_xxhash.js")
        installModuleFromResource(sql, "lz4", "/lz4.js")
        executeSqlFromResource(sql, "/lz4.sql")
        // Note: We know, that we do not need the replacements and code is faster without them!
        val replacements = mapOf("version" to version.toString(), "schema" to schema, "storage_id" to storageId)
        // Note: The compiler embeds the JBON classes into plv8.
        //       Therefore, we must not have it standalone, because otherwise we
        //       have two distinct instances in memory.
        //       A side effect sadly is that you need to require naksha, before you can require jbon!
        // TODO: Extend the commonjs2 code so that it allows to declare that one module contains another!
        installModuleFromResource(sql, "naksha", "/here-naksha-lib-plv8.js",
                beautify = true,
                replacements = replacements,
                extraCode =  """
plv8.moduleCache["base"] = module.exports["here-naksha-lib-base"];
plv8.moduleCache["jbon"] = module.exports["here-naksha-lib-jbon"];
module.exports = module.exports["here-naksha-lib-plv8"];
""")
        executeSqlFromResource(sql, "/naksha.sql", replacements)
        executeSqlFromResource(sql, "/jbon.sql")
        Static.createBaseInternalsIfNotExists(sql, schema, schemaOid)
        createInternalsIfNotExists(conn.pgConnection, schema, appName)
        conn.commit()
    }

    private fun createInternalsIfNotExists(conn: Connection, schema: String, appName: String) {
        val verifyCreation: (Response) -> Unit = {
            assert(it is SuccessResponse) { (it as ErrorResponse).reason.message }
        }
        val nakshaSession = startSession(conn, schema, appName, "", appName, null)

        val scTransaction = NakshaCollectionProxy(id = SC_TRANSACTIONS, partitions = PARTITION_COUNT_NONE, storageClass = SC_TRANSACTIONS, autoPurge = true, disableHistory = true)
        nakshaSession.write(WriteRequest(arrayOf(WriteFeature(NKC_TABLE, feature = scTransaction)))).let(verifyCreation)

        val scDictionaries = NakshaCollectionProxy(id = SC_DICTIONARIES, partitions = PARTITION_COUNT_NONE, storageClass = SC_DICTIONARIES, autoPurge = false, disableHistory = false)
        nakshaSession.write(WriteRequest(arrayOf(WriteFeature(NKC_TABLE, feature = scDictionaries)))).let(verifyCreation)

        val scIndices = NakshaCollectionProxy(id = SC_INDICES, partitions = PARTITION_COUNT_NONE, storageClass = SC_INDICES, autoPurge = false, disableHistory = false)
        nakshaSession.write(WriteRequest(arrayOf(WriteFeature(NKC_TABLE, feature = scIndices)))).let(verifyCreation)
    }
}