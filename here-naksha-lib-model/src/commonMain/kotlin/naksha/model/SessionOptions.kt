@file:Suppress("OPT_IN_USAGE")

package naksha.model

import naksha.base.fn.Fn3
import naksha.model.SessionOptions.SessionOptions_C.from
import naksha.model.objects.NakshaFeature
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.JsStatic
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Options when acquiring a new session.
 *
 * @constructor Creates a new session, if a default should be created, simply use [from], example `SessionOptions.from(null)`, which will create session options using the defaults setup in the current [NakshaContext] (this is the right thing to do, in most of the cases, and matches the default values in the Kotlin constructor).
 * @since 3.0
 */
@JsExport
data class SessionOptions @JvmOverloads constructor(
    /**
     * An arbitrary name used to identify this session in debug logs and monitoring tools.
     * @since 3.0
     */
    @JvmField
    val appName: String = NakshaContext.appName(),

    /**
     * The application that acts.
     * @since 3.0
     */
    @JvmField
    val appId: String = NakshaContext.appId(),

    /**
     * The author that acts; if any.
     * @since 3.0
     */
    @JvmField
    val author: String? = NakshaContext.author(),

    /**
     * Allow optimiser to execute requests in parallel, as long as it can provide similar guarantees that a single, not parallel session, would grant.
     *
     * Often this is not possible for writing, but for reading, where failures can be bypassed by simply repeating the operation or falling back to a single connection. Note that parallelization may use many connections for a single query, this can be a problem in some situations, where the storage only supports a limited number of total connections, and many clients want to read parallel. In these cases the feature can be disabled. Beware that this option is ignored when a parallel execution is forced via [ISession.executeParallel].
     * @since 3.0
     */
    @JvmField
    val parallel: Boolean = true,

    /**
     * Only use the master node to avoid replication lag, all writes will automatically hit the master.
     *
     * **This property should be avoided generally, it only is needed in very special rare cases!**
     * @since 3.0
     */
    @JvmField
    val useMaster: Boolean = false,

    /**
     * When calculating the hash of a feature, the paths that should be excluded from hash calculation.
     * @since 3.0
     */
    @JvmField
    val excludePaths: List<Array<String>>? = NakshaContext.currentContext().excludePaths,

    /**
     * When calculating the hash of a feature, a function to be called for every property to hash.
     *
     * The function receives the feature that is being hashed, the current path, and the value to be hashed (will be _null_, _String_, _Int_, _Long_, _Double_ or _Boolean_). It should return _true_, when the value should be part of the hash; _false_ otherwise.
     * @since 3.0
     */
    @JvmField
    val excludeFn: Fn3<Boolean, NakshaFeature, List<String>, Any?>? = NakshaContext.currentContext().excludeFn,

    /**
     * The time in milliseconds to wait for the TCP handshake.
     * @since 3.0
     */
    @JvmField
    val connectTimeout: Int = NakshaContext.currentContext().connectTimeout,

    /**
     * The time in milliseconds to wait for the TCP socket when reading or writing from it.
     * @since 3.0
     */
    @JvmField
    val socketTimeout: Int = NakshaContext.currentContext().socketTimeout,

    /**
     * The statement/request-timeout in milliseconds, this means how long to wait for each CREATE, UPDATE or DELETE to be executed.
     * @since 3.0
     */
    @JvmField
    val stmtTimeout: Int = NakshaContext.currentContext().stmtTimeout,

    /**
     * The lock-timeout in milliseconds, when the storage has to use locking.
     * @since 3.0
     */
    @JvmField
    val lockTimeout: Int = NakshaContext.currentContext().lockTimeout,

    /**
     * The timeout in milliseconds, when idle transactions are forcefully closed.
     * @since 3.0
     */
    @JvmField
    val idleTxTimeout: Int = NakshaContext.currentContext().idleTxTimeout,

    /**
     * Stream information.
     * @since 3.0
     */
    @JvmField
    val streamInfo: StreamInfo = NakshaContext.currentContext().streamInfo,

    /**
     * An authentication token for this session, if needed by the implementation.
     * @since 3.0
     */
    @JvmField
    val authToken: String? = null,

    /**
     * If the session should be used for debugging.
     *
     * The possible values are dependent on the implementation, and need to be supplied by the implementation. For example, for `lib-psql` you can log the queries and the real execution plans, for other implementations this may not be available, but maybe other debug hints.
     * @since 3.0
     */
    @JvmField
    val logLevel: String? = null,
) {
    /**
     * The stream-identifier for this session.
     * @since 3.0
     */
    val streamId: String = streamInfo.streamId

    /**
     * Returns the actor, which is either the [author], or if no [author] is available, the [appId].
     * @since 3.0
     */
    val actor: String
        get() = author ?: appId

    fun copyWithTimeouts(
        socketTimeout: Int? = null,
        connectTimeout: Int? = null,
        stmtTimeout: Int? = null,
        lockTimeout: Int? = null
    ): SessionOptions {
       return copy(
           socketTimeout = socketTimeout ?: this.socketTimeout,
           connectTimeout = connectTimeout ?: this.connectTimeout,
           stmtTimeout = stmtTimeout ?: this.stmtTimeout,
           lockTimeout = lockTimeout ?: this.lockTimeout,
       )
    }

    companion object SessionOptions_C {
        /**
         * Helper for JavaScript and Java to create a new default instance without providing too many arguments.
         * @param context the context, if being _null_, then [NakshaContext.currentContext] is called.
         * @param authToken the authentication-token to use, if any.
         * @param useMaster _true_ if the master node should be used forcefully (if supported by the storage); only necessary in rare situations, generally the default _false_ is recommended.
         * @param logLevel if logging should be enabled.
         * @return the session options.
         */
        @JvmStatic
        @JsStatic
        @JvmOverloads
        fun from(
            context: NakshaContext?,
            authToken: String? = null,
            useMaster: Boolean = false,
            logLevel: String? = Naksha.DEFAULT_SESSION_LOG_LEVEL
        ): SessionOptions {
            val c = context ?: NakshaContext.currentContext()
            return SessionOptions(
                appName = c.appName,
                appId = c.appId,
                author = c.author,
                excludePaths = c.excludePaths,
                excludeFn = c.excludeFn,
                connectTimeout = c.connectTimeout,
                socketTimeout = c.socketTimeout,
                stmtTimeout = c.stmtTimeout,
                lockTimeout = c.lockTimeout,
                useMaster = useMaster,
                streamInfo = c.streamInfo,
                authToken = authToken,
                logLevel = logLevel
            )
        }

        @JvmStatic
        @JsStatic
        @JsName("fromWithNullToken")
        fun from(context: NakshaContext, useMaster: Boolean): SessionOptions {
            return from(context, null, useMaster)
        }
    }
}
