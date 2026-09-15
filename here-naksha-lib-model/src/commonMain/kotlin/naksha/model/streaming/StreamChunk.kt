package naksha.model.streaming

import naksha.base.AtomicInt
import kotlin.js.JsExport
import kotlin.jvm.JvmName
import naksha.base.NakshaException

/**
 * A chunk of features with the guarantee that it does not contain multiple states of same feature _(so withe with the same [identifier][naksha.base.Id])_. Therefore, the chunk does not contain the same feature multiple times.
 *
 * The order of the provided features is not significant. They should be consumed and stored together in an atomic way. When features have been processed the steam must be notified by closing the chunk.
 * @since 3.0
 */
@JsExport
open class StreamChunk(
    /**
     * The stream to which this chunk belongs.
     * @since 3.0
     */
    @get:JvmName("stream")
    val stream: Stream,

    /**
     * The features being part of this chunk.
     * @since 3.0
     */
    @get:JvmName("features")
    val features: Array<StreamFeature>,

    /**
     * The amount of outstanding acknowledgements; defaults to `0`.
     *
     * The stream reading thread should add one for every processor and write that receives this chunk.
     * @see acknowledge
     * @see failed
     */
    @get:JvmName("acknowledgeCount")
    val acknowledgeCount: AtomicInt = AtomicInt(0)
) {
    /**
     * A thread safe failure reporting, called e.g. by [IStreamSession.write][naksha.model.IStreamSession.write] to report a failed write.
     *
     * Leaves [acknowledgeCount] unmodified and calls [Stream.failed].
     * @param retry _true_ if the stream should return the same chunk again to retry writing; _false_ if the writing failed finally and is not recoverable.
     * @since 3.0
     * @throws StreamException if [retry] is _false_ and the reading can be recovered later _(requires a new stream)_.
     * @throws NakshaException if [retry] is _false_, but the stream can't be recovered, therefore no recovery request is available.
     * @see naksha.model.IStreamSession.write
     */
    open fun failed(retry: Boolean) {
        stream._failed(chunk = this, retry = retry)
    }

    /**
     * A thread safe acknowledgement, called e.g. by [IStreamSession.write][naksha.model.IStreamSession.write] to acknowledge a successful write.
     *
     * Decrements the [acknowledgeCount], if reaching `0` notifies the [Stream] by calling [Stream.acknowledge]. The method is thread safe.
     * @since 3.0
     * @see naksha.model.IStreamSession.write
     */
    open fun acknowledge() {
        while (true) {
            val count = acknowledgeCount.get()
            if (count <= 0) return
            if (!acknowledgeCount.compareAndSet(count, count - 1)) continue
            if (count == 1) stream._acknowledge(this)
        }
    }

    /**
     * Returns a copy of this chunk with a new [acknowledgeCount], so reset to `1`.
     *
     * This method is intended to be used by the [Stream] to return a [StreamChunk] again.
     * @return a copy of this chunk.
     * @since 3.0
     */
    open fun copy(): StreamChunk = StreamChunk(stream, features.copyOf(), acknowledgeCount = AtomicInt(1))
}