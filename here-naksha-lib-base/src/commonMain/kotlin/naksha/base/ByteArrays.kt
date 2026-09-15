@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package naksha.base

/**
 * Static singleton providing typed read/write access directly on a [ByteArray].
 *
 * Each multibyte type comes in three endian variants:
 * - **no suffix** – native byte order of the host platform
 * - **`_be`** – big-endian (network byte order)
 * - **`_le`** – little-endian
 *
 * [getInt8] / [setInt8] have no endian variants because a single byte has no byte order.
 *
 * @since 3.0
 */
expect class ByteArrays {
    companion object ByteArraysCompanion {

        // --- float32 ---
        fun getFloat32(bytes: ByteArray, pos: Int): Float
        fun getFloat32Be(bytes: ByteArray, pos: Int): Float
        fun getFloat32Le(bytes: ByteArray, pos: Int): Float
        fun setFloat32(bytes: ByteArray, pos: Int, value: Float)
        fun setFloat32Be(bytes: ByteArray, pos: Int, value: Float)
        fun setFloat32Le(bytes: ByteArray, pos: Int, value: Float)

        // --- float64 ---

        fun getFloat64(bytes: ByteArray, pos: Int): Double
        fun getFloat64Be(bytes: ByteArray, pos: Int): Double
        fun getFloat64Le(bytes: ByteArray, pos: Int): Double
        fun setFloat64(bytes: ByteArray, pos: Int, value: Double)
        fun setFloat64Be(bytes: ByteArray, pos: Int, value: Double)
        fun setFloat64Le(bytes: ByteArray, pos: Int, value: Double)

        // --- int8 (no endian variants) ---

        fun getInt8(bytes: ByteArray, pos: Int): Byte
        fun setInt8(bytes: ByteArray, pos: Int, value: Byte)

        // --- int16 ---

        fun getInt16(bytes: ByteArray, pos: Int): Short
        fun getInt16Be(bytes: ByteArray, pos: Int): Short
        fun getInt16Le(bytes: ByteArray, pos: Int): Short
        fun setInt16(bytes: ByteArray, pos: Int, value: Short)
        fun setInt16Be(bytes: ByteArray, pos: Int, value: Short)
        fun setInt16Le(bytes: ByteArray, pos: Int, value: Short)

        // --- int32 ---

        fun getInt32(bytes: ByteArray, pos: Int): Int
        fun getInt32Be(bytes: ByteArray, pos: Int): Int
        fun getInt32Le(bytes: ByteArray, pos: Int): Int
        fun setInt32(bytes: ByteArray, pos: Int, value: Int)
        fun setInt32Be(bytes: ByteArray, pos: Int, value: Int)
        fun setInt32Le(bytes: ByteArray, pos: Int, value: Int)

        // --- int64 ---

        fun getInt64(bytes: ByteArray, pos: Int): Long
        fun getInt64Be(bytes: ByteArray, pos: Int): Long
        fun getInt64Le(bytes: ByteArray, pos: Int): Long
        fun setInt64(bytes: ByteArray, pos: Int, value: Long)
        fun setInt64Be(bytes: ByteArray, pos: Int, value: Long)
        fun setInt64Le(bytes: ByteArray, pos: Int, value: Long)
    }
}
