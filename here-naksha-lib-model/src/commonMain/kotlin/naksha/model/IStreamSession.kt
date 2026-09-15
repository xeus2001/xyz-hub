package naksha.model

import naksha.model.streaming.Stream
import naksha.model.streaming.StreamChunk
import naksha.model.streaming.StreamRequest
import kotlin.js.JsExport

/**
 * A session that can be used to stream data from a storage.
 * @since 3.0
 */
@JsExport
interface IStreamSession: ISession {
    /**
     * Opens a new stream to read from a collection in streaming mode.
     * @param request the request for the stream.
     * @return the stream.
     * @since 3.0
     */
    fun read(request: StreamRequest): Stream

    /**
     * Asks the storage to atomically create the features of the given chunk.
     *
     * The method will block the calling thread until all data is eventually writen into the storage with the consistency guarantees for the underlying implementation or the writing failed. The storage should consider that the provided features do exist already. It should not fail in such a case, except the existing features are in a modified state.
     *
     * The implementation can use a single connection for all writes or write each chunk using a dedicated own connection. However, the storage need to consider that it may get plenty of concurrent calls _(potentially thousands or millions when the reader uses virtual threads and enough memory is available)_. Therefore, the storage **must** prepare measurements to limit the amount of parallel writes, when necessary.
     *
     * The method must call either [StreamChunk.acknowledge] or [StreamChunk.failed].
     * @param chunk the chunk to persist, either a [StreamTransaction][naksha.model.streaming.StreamTransaction] or [StreamChunk].
     * @return _true_ if the chuck is stored and [acknowledged][StreamChunk.acknowledge]; _false_ if it [failed][StreamChunk.failed] writing.
     * @since 3.0
     * @see StreamChunk.acknowledge
     * @see StreamChunk.failed
     */
    fun write(chunk: StreamChunk): Boolean
}