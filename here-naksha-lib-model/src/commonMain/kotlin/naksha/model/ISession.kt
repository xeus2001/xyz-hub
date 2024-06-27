package naksha.model

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport


/**
 * When a session is opened, it is bound to the context in which the session shall operate. The read session will acquire a connection
 * from a connection pools when read is called, and release the connections directly after the read request was executed. The write session
 * will acquire the connection when the first read or write call is done and release it, when commit, rollback or close invoked. The
 * dictionary manager will grab an idle read or read/write connection on demand, and release it to the connection pool as soon as possible.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
interface ISession : AutoCloseable {
    /**
     * The [NakshaContext] bound to this session.
     */
    val context: NakshaContext

    /**
     * The statement timeout in milliseconds.
     */
    var stmtTimeout: Int

    /**
     * The statement timeout in milliseconds.
     */
    var lockTimeout: Int

    /**
     * Closing a session will invoke [IWriteSession.rollback] and return eventually still in use connections to their connection pool.
     */
    override fun close()
}
