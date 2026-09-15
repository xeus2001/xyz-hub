@file:Suppress("OPT_IN_USAGE")

package naksha.model

import kotlin.js.JsExport
import kotlin.jvm.JvmName

/**
 * When a session is opened, it is bound to the context in which the session shall operate.
 *
 * A read session will acquire a connection from a connection pools whenever a read is performed, and release the connections instantly after the read is done.
 *
 * A write session will acquire a connection when the first write operation is executed, and stick with it until `commit`, `rollback` or [close] invoked. All reads after write will always utilize this single connection to ensure consistency. Before the first write operation, the optimizer is free to utilize multiple connections to read in parallel, but after the first write execution, a single connection must be used for all reading and writing, to guarantee consistency. Therefore, it is recommended to first perform all reads, then to perform the writes. The parallel reading can be disabled, if needed, using the [SessionOptions.parallel] switch.
 */
@JsExport
interface ISession : AutoCloseable {
    /**
     * The storage to which the session is bound.
     * @since 3.0
     */
    val storage: IStorage

    /**
     * The socket timeout in milliseconds.
     * @since 3.0
     */
    var socketTimeout: Int

    /**
     * The statement/request-timeout in milliseconds.
     * @since 3.0
     */
    var stmtTimeout: Int

    /**
     * The lock timeout in milliseconds.
     * @since 3.0
     */
    var lockTimeout: Int

    /**
     * The options when opening new connections.
     *
     * The options are mostly immutable, except for the timeout values, for which there are dedicated setter.
     * @since 3.0
     */
    val options: SessionOptions

    /**
     * Tests if the session is closed.
     * @return _true_ if the session is closed.
     * @since 3.0
     */
    fun isClosed(): Boolean

    /**
     * Closing a session will roll back the underlying connection, and then return it to their connection pool. After closing a session
     * any further methods invocation will raise an [IllegalStateException].
     * @since 2.0.7
     */
    override fun close()
}