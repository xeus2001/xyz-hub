package naksha.model.streaming

import naksha.model.IStreamSession
import kotlin.js.JsExport
import kotlin.jvm.JvmName
import naksha.base.NakshaException
import kotlin.js.JsName
import kotlin.time.Duration

/**
 * A stream that can be read to replicate the content of a collection. The data being streaming is dependent on the [StreamRequest].
 *
 * **A stream should only be read by a single reader thread!**
 *
 * Generally all data is grouped by the stream. Reading is done in chunks being either [StreamChunk] or [StreamTransaction]. The method [next] will block until all chunks that need to be processed have been [acknowledged][acknowledge].
 *
 * The recommended way to process a stream is that one thread performs the reading of chunks and then delegates the actual write to worker threads, which should [acknowledge][StreamChunk.acknowledge] the writing at the [chunk][StreamChunk].
 *
 * The following is the recommended read loop:
 * ```kotlin
 * fun readAll(stream: Stream) {
 *   stream.use { try {
 *     while (true) {
 *       if (stream.hasMore()) {
 *         // Read ones, write multiple.
 *         val chunk = stream.next()
 *         // This is important here!
 *         chunk.acknowledgeCount.addAndGet(targets.size)
 *         for (target in targets) {
 *           doWrite(chunk, target)
 *         }
 *       } else {
 *         if (stream.isDone(5.seconds)) break
 *         // There may be a retry, due to
 *         // recoverable write failure!
 *       }
 *     }
 *     // We are done.
 *   } catch (StreamException e) {
 *     // Recover via e.recoveryRequest!
 *   } catch (NakshaException e) {
 *     // Failure without recovery!
 *   }}
 * }
 *
 * fun doWrite(
 *   chunck: StreamChunck,
 *   session: IStreamSession
 * ) {
 *   Thread.startVirtualThread {
 *     try {
 *       session.write(chunk)
 *     } catch (Exception e) {
 *       log.error("Write error", e)
 *       chunk.failed(retry=false)
 *     }
 *   }
 * }
 * ```
 *
 * This allows to read ones and write in parallel into multiple targets _(e.g. into S3 and multiple replication databases)_.
 *
 * #### Warning
 * For each writer the reader forwards the read [chunk][StreamChunk] to, it should increment the [acknowledgeCount][StreamChunk.acknowledgeCount] by one. This **must** be done before handing the chunk over to the first processor/writer! This is important, because otherwise a very fast writer could decrement the [acknowledgedCount][StreamChunk.acknowledgeCount] to `0` before all writers are done!
 *
 * This would cause a fatal _(and hard to find)_ error. The moment the [acknowledgedCount][StreamChunk.acknowledgeCount] goes to `0` it will notify the reader that the [chunk][StreamChunk] is finished, this causes the reader to proceed. If not all writers are yet done, this can cause great harm to consistency of the target storages.
 *
 * #### Note
 * The example code is based upon JVM version 24+, because between 21 _(including)_ and 24 _(excluding)_ the virtual threads have a severe bug with synchronized methods and synchronization blocks, see [JEPS-491](https://openjdk.org/jeps/491) and [JDK-8337395](https://bugs.openjdk.org/browse/JDK-8337395)!
 *
 * #### More details
 * It is the responsibility of the stream to decide in which order it is safe to consume chunks.
 *
 * For transactions, it is safe to process them in parallel if they do not impact each other. That means, as long as transactions do not contain the same features as any other yet unacknowledged transaction, they can be written in parallel.
 *
 * For example a Naksha storage will read the transaction log, then use multiple connections in parallel to read the transactions. It will hand them out in order, but [next] will only block, when the next transaction contains a feature that is as well part of a previously returned [StreamTransaction], which was not yet [acknowledged][acknowledge].
 *
 * It is the responsibility of the stream to decide in which order it is safe to consume the data. Specifically with disabled transactions, the stream implementation is free to reorder data. So it can read features, then re-group them so that as many chunks as possible can be written in parallel, only ensuring that no feature is processed in parallel. This can make the copy many times faster for certain storages. However, it means that features will move between transactions and therefore destroys the transactional order and the consistency between features.
 *
 * @since 3.0
 */
@JsExport
abstract class Stream(
    /**
     * The session to which this stream is bound.
     * @since 3.0
     */
    @get:JvmName("session")
    val session: IStreamSession,

    /**
     * The [request][StreamRequest] that was used to open the stream. This is **not** the recovery request, it is the original request that was used to establish the stream.
     * @since 3.0
     */
    @get:JvmName("request")
    val request: StreamRequest
) : Iterator<StreamChunk>, AutoCloseable {

    /**
     * An array of not yet acknowledged _(outstanding)_ [chunks][StreamChunk].
     *
     * This is intended only for logs or CLI tool reporting of the current status.
     * @since 3.0
     */
    abstract val unacknowledgedChunks: Array<StreamChunk>

    /**
     * An array of [chunks][StreamChunk] that have been acknowledged, but are pending. That means, the current restoration point was not yet forwarded, because some still outstanding [chunks][StreamChunk] prevent this. Ones the outstanding and blocking [chunks][StreamChunk] are acknowledged, the pending [chunks][StreamChunk] will be removed from this list.
     *
     * This is intended only for logs or CLI tool reporting of the current status.
     * @since 3.0
     */
    abstract val acknowledgedChunks: Array<StreamChunk>

    /**
     * The estimated total amount of chunks that will be streamed.
     *
     * This is only an educated guess by the storage, it can be adjusted while streaming. A storage can return `-1` if it has simply no clue what to expect _(or not yet a clue)_. This is intended only for logs or CLI tool reporting of the current status.
     * @since 3.0
     */
    abstract val estimatedChunks: Long

    /**
     * The estimated total amount of tuples _(feature states)_ that will be streamed.
     *
     * This is only an educated guess by the storage, it can be adjusted while streaming. A storage can return `-1` if it has simply no clue what to expect _(or not yet a clue)_. This is intended only for logs or CLI tool reporting of the current status.
     * @since 3.0
     */
    abstract val estimatedTuples: Long

    /**
     * The amount of tuple _(feature states)_ that have been successfully read and acknowledged by the processors.
     * @since 3.0
     */
    abstract val acknowledgedTuples: Long

    /**
     * The amount of tuple _(feature states)_ that have been read, but not yet fully acknowledged by the processors.
     * @since 3.0
     */
    abstract val unacknowledgedTuples: Long

    /**
     * If the stream is generally recoverable in an error case.
     *
     * If a stream is not recoverable, then a premature [close] of the stream should be avoided!
     * @since 3.0
     */
    abstract val isRecoverable: Boolean

    /**
     * Tests if there is another outstanding [StreamChunk] available.
     *
     * If this method returns _false_, the reader should [close] the stream.
     * @return _true_ if there is another [StreamChunk] available; _false_ otherwise.
     * @since 3.0
     * @see close
     */
    abstract override operator fun hasNext(): Boolean

    /**
     * Tests if the stream is finished and has no more outstanding [chunks][StreamChunk].
     *
     * If [hasNext] returns _false_, this method can be used to wait for all outstanding [chunks][StreamChunk] before calling [close]. Calling [close] directly is generally safe, but has a small risk, especially if the stream is not [recoverable][isRecoverable], because when writing of an outstanding [chunk][StreamChunk] fails, most often the writing can be retried.
     *
     * However, to retry, [next] need to be invoked again, but when [hasNext] returned _false_ there are no more [chunks][StreamChunk], except for the outstanding ones, which could fail writing and only then would become available again. This method closes this hole. When the method is invoked by the reading thread and returns _false_ after a short timeout, the caller should retry `hasNext()` to test if writing is just slow or a [chunk][StreamChunk] failed writing and can be retried. If that is the case, the reader thread should query this [chunk][StreamChunk] again via [next], and retry the writing.
     *
     * @param timeout the timeout after which to return with _false_. If `null`, less than or zero, the method must not block, but return instantly.
     * @return _true_ if there are still outstanding [chunks][StreamChunk]; _false_ otherwise.
     */
    abstract fun isDone(timeout: Duration?): Boolean

    /**
     * Returns the next [StreamFeature] or [StreamTransaction].
     * @return the next [StreamFeature] or [StreamTransaction].
     * @since 3.0
     * @throws NoSuchElementException if there are no more elements. The reader should invoke [close] to wait for outstanding [acknowledgements][acknowledge].
     * @throws NakshaException with error [ILLEGAL_STATE][naksha.base.NakshaError.ILLEGAL_STATE] if the stream is closed without any recovery possibility.
     * @throws StreamException if the stream is closed for an error reason, e.g. disconnect or timeout. Optionally can be recovered via [IStreamSession.read] using the [StreamException.recoveryRequest].
     */
    abstract override operator fun next(): StreamChunk

    /**
     * Returns the next [StreamFeature] or [StreamTransaction].
     *
     * The given timeout is no exact measurement, especially when given less than 1 second.
     * @param timeout the timeout after which to return with `null`. If `null`, less than or zero, the method must not block, but return instantly with either the next [chunk][StreamChunk] or `null`.
     * @return the next [StreamFeature] or [StreamTransaction] or `null`, if the timeout was reached.
     * @since 3.0
     * @throws NoSuchElementException if there are no more elements. The reader should invoke [close] to wait for outstanding [acknowledgements][acknowledge].
     * @throws NakshaException with error [ILLEGAL_STATE][naksha.base.NakshaError.ILLEGAL_STATE] if the stream is closed without any recovery possibility.
     * @throws StreamException if the stream is closed for an error reason, e.g. disconnect or timeout. Optionally can be recovered via [IStreamSession.read] using the [StreamException.recoveryRequest].
     */
    @JsName("nextOrNull")
    abstract fun next(timeout: Duration?): StreamChunk?

    /**
     * Called by [StreamChunk.acknowledge] to acknowledge that the chunk has been processed successfully, will potentially move the recovery position forward and allow reading more [chunks][StreamChunk], to potentially unblocks [Stream.next].
     *
     * If the same chunk is acknowledged multiple times, additional calls will simply be ignored.
     * @param chunk the [StreamChunk] chunk to flag as processed.
     * @since 3.0
     */
    protected abstract fun acknowledge(chunk: StreamChunk)
    @Suppress("FunctionName")
    internal fun _acknowledge(chunk: StreamChunk) = acknowledge(chunk)

    /**
     * Called by [StreamChunk.failed] to report that writing the given [StreamChunk] failed.
     *
     * The method only returns when [retry] is _true_ and the stream is able to return the given chunk again. For this to happen the stream must copy the [chunk]; otherwise the method always throws either a [NakshaException] or [StreamException].
     * @param chunk the [StreamChunk] that was not written successfully.
     * @param retry if writing can be retried.
     * @since 3.0
     * @see IStreamSession.write
     * @throws NakshaException if the stream was aborted, but the stream is not recoverable.
     * @throws StreamException if the stream was aborted successfully and the stream can be recovered.
     */
    protected abstract fun failed(chunk: StreamChunk, retry: Boolean)
    @Suppress("FunctionName")
    internal fun _failed(chunk: StreamChunk, retry: Boolean) = failed(chunk, retry)

    /**
     * Tests if this stream is closed.
     * @return _true_ if the stream is closed; _false_ otherwise.
     * @since 3.0
     */
    abstract fun isClosed(): Boolean

    /**
     * Closes the stream gracefully.
     *
     * When [hasNext] returns _false_, the reading thread should call this method to wait for all outstanding writes to [acknowledge], before eventually closing the stream. It as well can be used to intentionally close a stream gracefully.
     *
     * This method waits for [acknowledge] of all outstanding [chunks][StreamChunk], then closes the stream. If there are more [chunks][StreamChunk] available to read, it throws a [StreamException] with error [CLOSED][naksha.base.NakshaError.CLOSED], providing a recovery request. Should a [next] call be outstanding, it is interrupted by throwing the same exception.
     *
     * @since 3.0
     * @throws StreamException with a recovery request, if there are still outstanding chunks.
     * @throws NakshaException if recovery is not possible and there are outstanding chunks.
     */
    abstract override fun close()
}