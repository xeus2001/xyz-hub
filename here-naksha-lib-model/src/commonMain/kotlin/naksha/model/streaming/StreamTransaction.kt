package naksha.model.streaming

import naksha.base.AtomicInt
import naksha.base.Id
import naksha.base.Platform
import naksha.base.PlatformObject
import kotlin.js.JsExport
import kotlin.jvm.JvmName

/**
 * Describes a transaction.
 * @since 3.0
 */
@JsExport
open class StreamTransaction(
    /**
     * The stream to which the transaction belongs.
     * @since 3.0
     */
    stream: Stream,

    /**
     * The version that this transaction is linked to.
     *
     * A new version is always generated as result of committing a transaction, therefore, every transaction relates exactly to one version.
     * @since 3.0
     */
    @get:JvmName("version")
    val version: Long,

    /**
     * The identifier of the version, if the source storage has transaction logs.
     * @since 3.0
     */
    @get:JvmName("version")
    val id: Id?,

    /**
     * The transaction details, if the source storage has transaction logs.
     * @since 3.0
     */
    @get:JvmName("transaction")
    val transaction: PlatformObject?,

    /**
     * The features being part of this transaction.
     * @since 3.0
     */
    features: Array<StreamFeature>,

    /**
     * The amount of outstanding acknowledgements; defaults to `0`.
     *
     * The stream reading thread should add one for every processor and write that receives this chunk.
     * @since 3.0
     */
    acknowledgeCount: AtomicInt = AtomicInt(0)
): StreamChunk(stream, features, acknowledgeCount) {

    override fun copy(): StreamTransaction = StreamTransaction(
        stream,
        version,
        id,
        Platform.copy(transaction, true),
        features,
        acknowledgeCount
    )
}