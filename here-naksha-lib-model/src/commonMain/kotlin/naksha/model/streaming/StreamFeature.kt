package naksha.model.streaming

import naksha.base.Id
import naksha.base.PlatformMap
import naksha.base.TupleNumber
import kotlin.js.JsExport
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * A feature of a [StreamChunk].
 * @since 3.0
 * @see Stream
 */
@JsExport
open class StreamFeature @JvmOverloads constructor(
    /**
     * The [StreamChunk] to which this feature belongs.
     * @since 3.0
     */
    @get:JvmName("chunk")
    val chunk: StreamChunk,

    /**
     * The identifier of the feature.
     * @since 3.0
     */
    @get:JvmName("id")
    val id: Id,

    /**
     * The feature itself.
     * @since 3.0
     */
    @get:JvmName("feature")
    val feature: PlatformMap,

    /**
     * The optional type of the feature, if provided by the source storage.
     * @since 3.0
     */
    @get:JvmName("featureType")
    val featureType: String? = null,

    /**
     * The [TupleNumber] of the feature, if the source storage does provide this.
     *
     * If the target system contains the same database
     * @since 3.0
     */
    @get:JvmName("tupleNumber")
    val tupleNumber: TupleNumber? = null,
)