@file:Suppress("OPT_IN_USAGE")

package naksha.base

import naksha.base.fn.Fx2
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.JsStatic
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * An error class.
 * @since 3.0.0
 */
@JsExport
open class NakshaError() : AnyObject() {

    /**
     * Create a new error from the given arguments.
     * @param code the error code.
     * @param msg a human-readable message.
     * @param cause the origin exception that caused this error; if any.
     */
    @JsName("of")
    @JvmOverloads
    constructor(code: String, msg: String, cause: Throwable? = null) : this() {
        this.code = code
        this.msg = msg
        this.cause = cause
    }

    companion object NakshaErrorCompanion {
        /**
         * A socket, connection, stream or alike was or is closed. Dependent on context, this can be as well a success error, for example when `close` was called on a stream intentionally.
         * @since 3.0.0
         */
        const val CLOSED = "Closed"

        /**
         * A general error (not further specified) happened while processing the request.
         *
         * This results in a 500 Internal Server Error.
         * @since 3.0.0
         */
        const val EXCEPTION = "Exception"

        /**
         * Thrown when a state is found that must not be found, for example some other part of the code should have prevented this state at this point.
         *
         * This results in a 500 Internal Server Error.
         * @since 3.0.0
         */
        const val INTERNAL_ERROR = "InternalError"

        /**
         * Returned when an already initialized storage is initialized, providing a wrong _storage-id_ and/or _storage-number_.
         * @since 3.0.0
         */
        const val STORAGE_ID_MISMATCH = "StorageIdMismatch"

        /**
         * Returned when something requires initialisation before a certain method can be invoked.
         * @since 3.0.0
         */
        const val UNINITIALIZED = "Uninitialized"

        /**
         * Returned when initialisation failed.
         * @since 3.0.0
         */
        const val INITIALIZATION_FAILED = "InitializationFailed"

        /**
         * A provided identifier is not allowed.
         * @since 3.0.0
         */
        const val ILLEGAL_ID = "IllegalId"

        /**
         * Returned when trying to create a collection that exists already.
         * @since 3.0.0
         */
        const val COLLECTION_EXISTS = "CollectionExists"

        /**
         * The collection accessed does not exist.
         * @since 3.0.0
         */
        const val COLLECTION_NOT_FOUND = "CollectionNotFound"

        /**
         * A specific partition was not found.
         * @since 3.0.0
         */
        const val PARTITION_NOT_FOUND = "PartitionNotFound"

        /**
         * An event that was sent through a space and failed, because a handler cannot process it.
         *
         * This will result in an `501 Not Implemented` response.
         * @since 3.0.0
         */
        const val NOT_IMPLEMENTED = "NotImplemented"

        /**
         * An error being thrown when an internal API call is not supported by the implementation.
         *
         * This will result in an `500 Internal Server Error` response.
         * @since 3.0.0
         */
        const val UNSUPPORTED_OPERATION = "UnsupportedOperation"

        /**
         * A not further specified conflict occurred when performing an operation, for example when the database reports a unique index violation, and the storage is not able to give a more specific reason.
         * @since 3.0.0
         * @see [isConflict]
         * @see [CATALOG_EXISTS]
         * @see [MAP_NOT_FOUND]
         * @see [COLLECTION_EXISTS]
         * @see [COLLECTION_NOT_FOUND]
         * @see [FEATURE_EXISTS]
         * @see [FEATURE_NOT_FOUND]
         */
        const val CONFLICT = "Conflict"

        /**
         * Indicates an authorization error.
         *
         * This will result in a 401 Unauthorized response.
         * @since 3.0.0
         */
        const val UNAUTHORIZED = "Unauthorized"

        /**
         * Indicates an authorization error.
         *
         * This will result in an 403 Forbidden response.
         * @since 3.0.0
         */
        const val FORBIDDEN = "Forbidden"

        /**
         * The connector cannot handle the request due to a processing limitation in an upstream service or a database.
         *
         * This will result in an 429 Too Many Requests response.
         * @since 3.0.0
         */
        const val TOO_MANY_REQUESTS = "TooManyRequests"

        /**
         * Returned by the storage, if too many connections are established already.
         *
         * This should result in an 429 Too Many Requests response.
         * @since 3.0.0
         */
        const val TOO_MANY_CONNECTIONS = "TooManyConnections"

        /**
         * A provided argument is invalid or missing.
         *
         * This will lead to an HTTP 400 Bad Request response.
         * @since 3.0.0
         */
        const val ILLEGAL_ARGUMENT = "IllegalArgument"

        /**
         * Something is expected in a specific state, but was found differently.
         *
         * This will lead to an HTTP 500 Internal Server Error response.
         * @since 3.0.0
         */
        const val ILLEGAL_STATE = "IllegalState"

        /**
         * Any service or remote function required to process the request was not reachable.
         *
         * This will result in a 502 Bad Gateway response.
         * @since 3.0.0
         */
        const val BAD_GATEWAY = "BadGateway"

        /**
         * The request was aborted due to a timeout.
         *
         * This will result in an HTTP 504 Gateway Timeout response.
         * @since 3.0.0
         */
        const val TIMEOUT = "Timeout"

        /**
         * The request was aborted due to PayloadTooLarge.
         *
         * This will result in an HTTP 513 response.
         * @since 3.0.0
         */
        const val PAYLOAD_TOO_LARGE = "PayloadTooLarge"

        /**
         * The requested feature was not available.
         *
         * This will result in an HTTP 404 response.
         * @since 3.0.0
         */
        const val NOT_FOUND = "NotFound"

        /**
         * A feature does exist, but is expected to not exist.
         *
         * @since 3.0.0
         */
        const val FEATURE_EXISTS = "FeatureExists"

        /**
         * A feature does not exist, but is expected to exist.
         *
         * @since 3.0.0
         */
        const val FEATURE_NOT_FOUND = "FeatureNotFound"

        /**
         * A map does exist, but is expected to not exist.
         *
         * @since 3.0.0
         */
        const val CATALOG_EXISTS = "MapExists"

        /**
         * A map does not exist, but is expected to exist.
         *
         * @since 3.0.0
         */
        const val MAP_NOT_FOUND = "MapNotFound"

        /**
         * A [dictionary-manager][naksha.jbon.IDictManager] does not exist, but is expected to exist.
         *
         * @since 3.0.0
         */
        const val DICT_MANAGER_NOT_FOUND = "DictManagerNotFound"

        /**
         * A storage does not exist, but is expected to exist.
         *
         * @since 3.0.0
         */
        const val STORAGE_NOT_FOUND = "StorageNotFound"

        /**
         * An error to be reported by the storage, if the `id` is unique, but another feature with the same feature-number exists. This is a hash collision as described in [Id]. The storage will not resolve this, it will only inform the client about this problem, the client has to use a different `id` or relocate the feature into another collection.
         * @since 3.0
         * @see Id
         */
        const val ID_COLLISION = "IdCollision"

        private val CODE_FIELD = NotNullProperty<NakshaError, String>(String::class) { _, _ -> EXCEPTION }
        private val MSG_FIELD = NotNullProperty<NakshaError, String>(String::class) { self, _ -> self.code }
        private val THROWABLE_FIELD = NullableProperty<NakshaError, Throwable>(Throwable::class)

        /**
         * The method that is invoked, when [print] is invoked. If `null`, then calling [print] does nothing.
         * @since 3.0
         */
        @JsStatic
        @JvmStatic
        val printer = AtomicRef<Fx2<NakshaError, PlatformLogger>>(Fx2 { err, logger ->
            var cause = err.cause
            while (cause?.cause != null) cause = cause.cause
            if (cause != null) {
                logger.info("{} {}, cause: {}", err.code, err.msg, cause)
            } else {
                logger.info("{} {}", err.code, err.msg)
            }
        })
    }

    /**
     * The error code.
     */
    var code by CODE_FIELD

    /**
     * A human-readable message.
     */
    var msg by MSG_FIELD

    /**
     * The origin exception that caused this error; if any.
     */
    var cause by THROWABLE_FIELD

    /**
     * Tests if this error represents some kind of conflict and should lead to an 409 Conflict response.
     * @return `true` if this error represents a conflict; `false` otherwise.
     * @since 3.0
     */
    fun isConflict(): Boolean = when(code) {
        CATALOG_EXISTS,
        MAP_NOT_FOUND,
        COLLECTION_EXISTS,
        COLLECTION_NOT_FOUND,
        FEATURE_EXISTS,
        FEATURE_NOT_FOUND,
        CONFLICT -> true
        else -> false
    }

    override fun hashCode(): Int = code.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is NakshaError
                && code == other.code
                && msg == other.msg
                && cause == other.cause
    }

    override fun toString(): String = "NakshaError(code=$code, msg=$msg)"

    /**
     * Send this error to the logger.
     * @param logger the logger to which to send if `null`, the [Platform.logger] is used.
     */
    @JvmOverloads
    open fun print(logger: PlatformLogger = Platform.logger) {
        printer.get()?.call(this, logger)
    }
}

