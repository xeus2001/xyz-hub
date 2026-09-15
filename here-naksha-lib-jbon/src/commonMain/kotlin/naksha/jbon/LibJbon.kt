// This will be exposed
// - in JavaScript at the namespace: naksha.jbon.{name}
// - jn Java at the class naksha.jbon.LibJbonKt.{name}
package naksha.jbon

import naksha.base.DataViewProxy

// Encoding constants
internal const val ENC_MASK = 0b1100_0000
internal const val ENC_MIXED = 0b0000_0000
internal const val ENC_TINY = 0b0100_0000
internal const val ENC_STRING = 0b1000_0000
internal const val ENC_STRUCT = 0b1100_0000

// Encode as ENC_TINY_INT or (value and 0x1f)
// Decode as ((value shl 27) shr 27)
internal const val ENC_TINY_MASK = 0b1_00000
internal const val ENC_TINY_INT = 0b0_00000
internal const val ENC_TINY_FLOAT = 0b1_00000

internal const val ENC_MIXED_MASK = 0b1111_0000
internal const val ENC_MIXED_CORS = 0b0000_0000 // Constant OR Scalar
internal const val ENC_MIXED_REF5_LOCAL = 0b0001_0000
internal const val ENC_MIXED_REF5_GLOBAL = 0b0010_0000
internal const val ENC_MIXED_REF = 0b0011_0000 // 1, 2 or 4 byte payload

// If this is a back-reference: (leadIn and ENC_MIXED_REF_BACK_BIT) == ENC_MIXED_REF_BACK_BIT
internal const val ENC_MIXED_REF_BACK_BIT = 0b1000
// If this is a global-reference: (leadIn and ENC_MIXED_REF_GLOBAL_BIT) == ENC_MIXED_REF_GLOBAL_BIT
internal const val ENC_MIXED_REF_GLOBAL_BIT = 0b0100
// Mask the reference type: (leadIn and ENC_MIXED_REF_TYPE_MASK)
internal const val ENC_MIXED_REF_TYPE_MASK = 0b1111_0011
internal const val ENC_MIXED_REF_NULL = ENC_MIXED_REF or 0b0000
internal const val ENC_MIXED_REF_INT8 = ENC_MIXED_REF or 0b0001
internal const val ENC_MIXED_REF_INT16 = ENC_MIXED_REF or 0b0010
internal const val ENC_MIXED_REF_INT32 = ENC_MIXED_REF or 0b0011

internal const val ENC_MIXED_CONST_UNDEFINED = ENC_MIXED_CORS or 0b0000
internal const val ENC_MIXED_CONST_NULL = ENC_MIXED_CORS or 0b0001
internal const val ENC_MIXED_CONST_FALSE = ENC_MIXED_CORS or 0b0010
internal const val ENC_MIXED_CONST_TRUE = ENC_MIXED_CORS or 0b0011
internal const val ENC_MIXED_SCALAR_INT8 = ENC_MIXED_CORS or 0b0100 // 1 byte payload
internal const val ENC_MIXED_SCALAR_INT16 = ENC_MIXED_CORS or 0b0101 // 2 byte payload
internal const val ENC_MIXED_SCALAR_INT32 = ENC_MIXED_CORS or 0b0110 // 4 byte payload
internal const val ENC_MIXED_SCALAR_INT64 = ENC_MIXED_CORS or 0b0111 // 8 byte payload
internal const val ENC_MIXED_SCALAR_FLOAT16 = ENC_MIXED_CORS or 0b1000 // 2 byte payload
internal const val ENC_MIXED_SCALAR_FLOAT32 = ENC_MIXED_CORS or 0b1001 // 4 byte payload
internal const val ENC_MIXED_SCALAR_FLOAT64 = ENC_MIXED_CORS or 0b1010 // 8 byte payload
internal const val ENC_MIXED_SCALAR_FLOAT128 = ENC_MIXED_CORS or 0b1011 // 16 byte payload
internal const val ENC_MIXED_SCALAR_TIMESTAMP = ENC_MIXED_CORS or 0b1100 // 6 byte payload
internal const val ENC_MIXED_RESERVED1 = ENC_MIXED_CORS or 0b1101
internal const val ENC_MIXED_RESERVED2 = ENC_MIXED_CORS or 0b1110
internal const val ENC_MIXED_RESERVED3 = ENC_MIXED_CORS or 0b1111

internal const val ENC_STRUCT_SIZE_MASK = 0b0011_0000
internal const val ENC_STRUCT_SIZE0 = 0b00_0000
internal const val ENC_STRUCT_SIZE8 = 0b01_0000
internal const val ENC_STRUCT_SIZE16 = 0b10_0000
internal const val ENC_STRUCT_SIZE32 = 0b11_0000

internal const val ENC_STRUCT_VARIANT_MASK = 0b0000_1100
internal const val ENC_STRUCT_VARIANT0 = 0b0000 // no variant
internal const val ENC_STRUCT_VARIANT8 = 0b0100
internal const val ENC_STRUCT_VARIANT16 = 0b1000
internal const val ENC_STRUCT_VARIANT32 = 0b1100

internal const val ENC_STRUCT_TYPE_MASK = 0b0000_0011
internal const val ENC_STRUCT_ARRAY = 0b00 // no variant
internal const val ENC_STRUCT_MAP = 0b01 // no variant
internal const val ENC_STRUCT_DICTIONARY = 0b10 // no variant
internal const val ENC_STRUCT_RESERVED = 0b11 // no variant
internal const val ENC_STRUCT_VARIANT_FEATURE = 0b00
internal const val ENC_STRUCT_VARIANT_XYZ = 0b01
internal const val ENC_STRUCT_VARIANT_CUSTOM = 0b10
internal const val ENC_STRUCT_VARIANT_RESERVED = 0b11

// Public types.
const val CLASS_MASK = 0b1111_0000
const val CLASS_SCALAR = 0b0001_0000
const val CLASS_STRING = 0b0010_0000
const val CLASS_STRUCT = 0b0100_0000

const val TYPE_NULL = CLASS_SCALAR or 0b0000
const val TYPE_UNDEFINED = CLASS_SCALAR or 0b0001
const val TYPE_BOOL = CLASS_SCALAR or 0b0010
const val TYPE_INT = CLASS_SCALAR or 0b0011
const val TYPE_FLOAT = CLASS_SCALAR or 0b0100
const val TYPE_REF = CLASS_SCALAR or 0b0101
const val TYPE_TIMESTAMP = CLASS_SCALAR or 0b0110 // UTC epoch in milliseconds

const val TYPE_STRING = CLASS_STRING or 0b0000

// without variant
const val TYPE_ARRAY = CLASS_STRUCT or 0b0000
const val TYPE_MAP = CLASS_STRUCT or 0b0001
const val TYPE_DICTIONARY = CLASS_STRUCT or 0b0010
const val TYPE_RESERVED = CLASS_STRUCT or 0b0111
// with variant
const val TYPE_FEATURE = CLASS_STRUCT or 0b0100
const val TYPE_RESERVED1 = CLASS_STRUCT or 0b0101
const val TYPE_RESERVED2 = CLASS_STRUCT or 0b0110
const val TYPE_CUSTOM = CLASS_STRUCT or 0b0111

const val FEATURE_VARIANT_UNKNOWN = 0
const val FEATURE_VARIANT_GEO_JSON = 1
const val FEATURE_VARIANT_TAGS = 2

/**
 * A special type returned when the offset in a reader is invalid or for any other error.
 */
const val EOF = -1
internal const val ADD_NOTHING = 0b00
internal const val ADD_SPACE = 0b01
internal const val ADD_UNDERSCORE = 0b10
internal const val ADD_COLON = 0b11

@Suppress("NOTHING_TO_INLINE")
inline fun newDataView(size: Int) = DataViewProxy(size)

fun Exception.rootCause(): Exception {
    var e = this
    while (true) {
        val cause = e.cause
        if (cause == null || cause == e || !(cause is Exception)) return e
        e = cause
    }
}