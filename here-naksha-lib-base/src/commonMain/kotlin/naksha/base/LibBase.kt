@file:Suppress("NOTHING_TO_INLINE", "unused")

// This will be exposed
// - in JavaScript at the namespace: naksha.base.{name}
// - jn Java at the class naksha.base.LibBaseKt.{name}
package naksha.base

import kotlin.js.JsExport
import kotlin.reflect.KClass
import kotlin.reflect.cast

fun asInt64(any: Any?): Long = any as Long

const val INT_TO_UNSIGNED_INT64_MASK = 0xffff_ffffL

inline fun Double.toInt64RawBits(): Long = toRawBits()
inline fun Double.toLongRawBits(): Long = toRawBits()
inline fun Double.toInt64(): Long = Platform.toInt64(this)
inline fun Long.toInt64(): Long = this
inline fun Int.toInt64(): Long = toLong()

/**
 * Convert the integer into an unsigned 64-bit integer, so `-1` becomes `4294967295` _(aka `0xffffffff`)_.
 */
inline fun Int.toUnsignedInt64(): Long = toLong() and INT_TO_UNSIGNED_INT64_MASK

inline fun <K : Any, V : Any> AtomicMap(): AtomicMap<K, V> = Platform.newAtomicMap()
inline fun AtomicBool(initialValue: Boolean = false): AtomicBool = Platform.newAtomicBool(initialValue)
inline fun AtomicInt(initialValue: Int = 0): AtomicInt = Platform.newAtomicInt(initialValue)
inline fun AtomicInt64(initialValue: Long = 0): AtomicInt64 = Platform.newAtomicInt64(initialValue)
inline fun <T : Any> AtomicRef(referee: T?): AtomicRef<T> = Platform.newAtomicRef(referee)
inline fun <T : Any> AtomicNonNullRef(referee: T): AtomicNonNullRef<T> = Platform.newAtomicNonNullRef(referee)
inline fun <T : Any> WeakRef(referee: T): WeakRef<T> = Platform.newWeakRef(referee)

/**
 * Cast this, if possible, into the given proxy.
 * @param klass the proxy class to return.
 * @return this as the requested proxy or `null`.
 * @since 3.0
 */
@Suppress("UNCHECKED_CAST")
fun <T : Proxy> Any?.proxy(klass: KClass<T>): T? {
    if (this == null) return null
    if (klass.isInstance(this)) return this as T
    if (this is PlatformMap) return this.proxy(klass)
    if (this is Proxy) return this.proxy(klass)
    return null
}

/**
 * Remove the given element from the array, if it was contained in the array.
 * @param element the element to remove.
 * @return the new array or this, if the element was not part of the array.
 */
inline operator fun <reified T> Array<T>.minus(element: T?): Array<T> {
    val i = indexOf(element)
    if (i < 0) return this
    var si = 0
    val newArray = arrayOfNulls<T>(size - 1)
    var ni = 0
    while (si < size) {
        if (si != i) newArray[ni++] = this[si]
        si++
    }
    @Suppress("UNCHECKED_CAST")
    return newArray as Array<T>
}


/**
 * The maximum length of identifiers _(`42`)_.
 * @since 3.0
 */
@JsExport
const val MAX_ID_LENGTH = 42 // The answer to everything ;-)

/**
 * The maximum length of internal identifiers.
 * @since 3.0
 */
@JsExport
const val MAX_INTERNAL_ID_LENGTH = 63

/** Constant for the Naksha prefix that is reserved, no identifier must start with it: `naksha` */
@JsExport
const val INTERNAL_PREFIX = "naksha~"

/** Constant of the database `type` text */
@JsExport
const val DATABASE_TYPE = "naksha.Database"

/** Constant of the catalog `type` text */
@JsExport
const val CATALOG_TYPE = "naksha.Catalog"

/** Constant of the collection `type` text */
@JsExport
const val COLLECTION_TYPE = "naksha.Collection"

/** Constant of the feature `type` text */
@JsExport
const val FEATURE_TYPE = "naksha.Feature"

/** Constant of the transaction `type` text */
@JsExport
const val TRANSACTION_TYPE = "naksha.Tx"

/** Constant of the book `type` text */
@JsExport
const val BOOK_TYPE = "naksha.Book"

/** Constant of the member `type` text */
@JsExport
const val MEMBER_TYPE = "naksha.Member"

/** Constant of the index `type` text */
@JsExport
const val INDEX_TYPE = "naksha.Index"

// ── Well-known internal identifiers ──────────────────────────────────

/** Text of the administration catalog identifier (`naksha~admin`). */
@JsExport
const val ADMIN_CATALOG_TEXT = "${INTERNAL_PREFIX}admin"
/** Quoted text of the administration catalog identifier (`"naksha~admin"`). */
@JsExport
const val ADMIN_CATALOG_QUOTED = "\"${INTERNAL_PREFIX}admin\""
/** Number of the administration catalog (fixed to `0`). */
@JsExport
const val ADMIN_CATALOG_NUMBER = 0L

/** Text of the collections-collection identifier (`naksha~collections`). */
@JsExport
const val COLLECTIONS_COL_TEXT = "${INTERNAL_PREFIX}collections"
/** Quoted text of the collections-collection identifier (`"naksha~collections"`). */
@JsExport
const val COLLECTIONS_COL_QUOTED = "\"${INTERNAL_PREFIX}collections\""
/** Number of the collections-collection (fixed to `0`). */
@JsExport
const val COLLECTIONS_COL_NUMBER = 0L

/** Text of the transactions-collection identifier (`naksha~transactions"`). */
@JsExport
const val TRANSACTIONS_COL_TEXT = "${INTERNAL_PREFIX}transactions"
/** Quoted text of the transactions-collection identifier (`"naksha~transactions"`). */
@JsExport
const val TRANSACTIONS_COL_QUOTED = "\"${INTERNAL_PREFIX}transactions\""
/** Number of the transactions-collection (fixed to `1`). */
@JsExport
const val TRANSACTIONS_COL_NUMBER = 1L

/** Text of the catalogs-collection identifier (`naksha~catalogs`). */
@JsExport
const val CATALOGS_COL_TEXT = "${INTERNAL_PREFIX}catalogs"
/** Quoted text of the catalogs-collection identifier (`"naksha~catalogs"`). */
@JsExport
const val CATALOGS_COL_QUOTED = "\"${INTERNAL_PREFIX}catalogs\""
/** Number of the catalogs-collection (fixed to `2`). */
@JsExport
const val CATALOGS_COL_NUMBER = 2L

/** Text of the books-collection identifier (`naksha~books`). */
@JsExport
const val BOOKS_COL_TEXT = "${INTERNAL_PREFIX}books"
/** Quoted text of the books-collection identifier (`"naksha~books"`). */
@JsExport
const val BOOKS_COL_QUOTED = "\'${INTERNAL_PREFIX}books\'"
/** Number of the books-collection (fixed to `3`). */
@JsExport
const val BOOKS_COL_NUMBER = 3L




// -------------------------------------------------------------------------------------------------
// ByteArray typed accessors — thin inline wrappers around ByteArrays
// -------------------------------------------------------------------------------------------------

// float32

inline fun ByteArray.getFloat32(pos: Int): Float    = ByteArrays.getFloat32(this, pos)
inline fun ByteArray.getFloat32Be(pos: Int): Float  = ByteArrays.getFloat32Be(this, pos)
inline fun ByteArray.getFloat32Le(pos: Int): Float  = ByteArrays.getFloat32Le(this, pos)
inline fun ByteArray.setFloat32(pos: Int, value: Float)   { ByteArrays.setFloat32(this, pos, value) }
inline fun ByteArray.setFloat32Be(pos: Int, value: Float) { ByteArrays.setFloat32Be(this, pos, value) }
inline fun ByteArray.setFloat32Le(pos: Int, value: Float) { ByteArrays.setFloat32Le(this, pos, value) }

// float64

inline fun ByteArray.getFloat64(pos: Int): Double    = ByteArrays.getFloat64(this, pos)
inline fun ByteArray.getFloat64Be(pos: Int): Double  = ByteArrays.getFloat64Be(this, pos)
inline fun ByteArray.getFloat64Le(pos: Int): Double  = ByteArrays.getFloat64Le(this, pos)
inline fun ByteArray.setFloat64(pos: Int, value: Double)   { ByteArrays.setFloat64(this, pos, value) }
inline fun ByteArray.setFloat64Be(pos: Int, value: Double) { ByteArrays.setFloat64Be(this, pos, value) }
inline fun ByteArray.setFloat64Le(pos: Int, value: Double) { ByteArrays.setFloat64Le(this, pos, value) }

// int8 (no endian variants)

inline fun ByteArray.getInt8(pos: Int): Byte       = ByteArrays.getInt8(this, pos)
inline fun ByteArray.setInt8(pos: Int, value: Byte) { ByteArrays.setInt8(this, pos, value) }

// int16

inline fun ByteArray.getInt16(pos: Int): Short    = ByteArrays.getInt16(this, pos)
inline fun ByteArray.getInt16Be(pos: Int): Short  = ByteArrays.getInt16Be(this, pos)
inline fun ByteArray.getInt16Le(pos: Int): Short  = ByteArrays.getInt16Le(this, pos)
inline fun ByteArray.setInt16(pos: Int, value: Short)   { ByteArrays.setInt16(this, pos, value) }
inline fun ByteArray.setInt16Be(pos: Int, value: Short) { ByteArrays.setInt16Be(this, pos, value) }
inline fun ByteArray.setInt16Le(pos: Int, value: Short) { ByteArrays.setInt16Le(this, pos, value) }

// int32

inline fun ByteArray.getInt32(pos: Int): Int    = ByteArrays.getInt32(this, pos)
inline fun ByteArray.getInt32Be(pos: Int): Int  = ByteArrays.getInt32Be(this, pos)
inline fun ByteArray.getInt32Le(pos: Int): Int  = ByteArrays.getInt32Le(this, pos)
inline fun ByteArray.setInt32(pos: Int, value: Int)   { ByteArrays.setInt32(this, pos, value) }
inline fun ByteArray.setInt32Be(pos: Int, value: Int) { ByteArrays.setInt32Be(this, pos, value) }
inline fun ByteArray.setInt32Le(pos: Int, value: Int) { ByteArrays.setInt32Le(this, pos, value) }

// int64

inline fun ByteArray.getInt64(pos: Int): Long    = ByteArrays.getInt64(this, pos)
inline fun ByteArray.getInt64Be(pos: Int): Long  = ByteArrays.getInt64Be(this, pos)
inline fun ByteArray.getInt64Le(pos: Int): Long  = ByteArrays.getInt64Le(this, pos)
inline fun ByteArray.setInt64(pos: Int, value: Long)   { ByteArrays.setInt64(this, pos, value) }
inline fun ByteArray.setInt64Be(pos: Int, value: Long) { ByteArrays.setInt64Be(this, pos, value) }
inline fun ByteArray.setInt64Le(pos: Int, value: Long) { ByteArrays.setInt64Le(this, pos, value) }
