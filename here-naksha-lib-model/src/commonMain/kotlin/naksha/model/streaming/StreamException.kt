package naksha.model.streaming

import naksha.base.NakshaError
import naksha.base.NakshaException
import naksha.model.IStreamSession
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * An exception thrown by a [Stream] if the reading or [acknowledgement][Stream.acknowledge] failed. The exception contains the [StreamRequest] to recover from the error. If the error is not recoverable, then only a [NakshaException] will be thrown.
 * @since 3.0
 */
@JsExport
open class StreamException(
    /**
     * A recovery request that can be used to invoke [IStreamSession.read] to recovery the aborted [stream] from the current position, if the [stream] is recoverable.
     *
     * ### Warning
     * When writing in parallel, the recovery need care by the consumer. It needs to be aware that when recovering, some writes are duplicates. The stream implementation will only guarantee read ordering, not write order.
     *
     * Therefore, it can happen that the recovery point is behind the current write _HEAD_. For example, assume a stream starts at version `0`, and chunks `1`, `2`, `3`, and `4` are read and written in parallel in four worker threads, because they do not intersect. Now worker #1 acknowledges chunk `1`, this will update the stream read position to `1`. Now, assume worker `3` and `4` next confirm their writes. This will **not** update forward stream read position, because the confirmation for chunk `2` is still outstanding. When the read or the write of stream #2 fails, the [recoveryRequest] still be stuck at read position `1`, with chunks `3` and `4` already being written.
     *
     * When the [recoveryRequest] is then stored to resume the operation, it will start at chunk `2`. This will then read chunk `2`, `3`, and `4` again, of which `3` and `4` have been written already! This is the responsibility of the consumer to either skip writes `3` or `4`, or to ignore the write errors due to the fact that these features do exist already.
     * @since 3.0
     * @see Stream
     */
    val recoveryRequest: StreamRequest,

    /**
     * The stream that caused the exception, it will be closed now.
     * @since 3.0
     */
    val stream: Stream,

    /**
     * The error reason to forward to super class.
     * @since 3.0
     */
    error: NakshaError
) : NakshaException(error) {

    /**
     * Create a recovery stream exception based upon individual values, which will be assembled to an [NakshaError].
     * @param recoverOptions tThe options that can be used to recover the stream using a new [IReadSession][naksha.model.IReadSession].
     * @param stream the [Stream] that raised this exception.
     * @param code the error code, put into [NakshaError.code].
     * @param msg the human-readable error message, put into [message] and into [NakshaError.msg].
     * @param cause the optional cause of this error, put into [Exception.cause].
     * @since 3.0
     */
    @JsName("newStreamException")
    constructor(recoverOptions: StreamRequest, stream: Stream, code: String, msg: String, cause: Throwable? = null)
            : this(recoverOptions, stream, NakshaError(code, msg, cause))
}