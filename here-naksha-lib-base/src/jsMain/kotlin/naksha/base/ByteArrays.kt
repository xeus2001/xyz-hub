@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "OPT_IN_USAGE")

package naksha.base

/**
 * JS actual implementation of [ByteArrays].
 *
 * In JavaScript a [ByteArray] is an `Int8Array` backed by an `ArrayBuffer`. To read/write
 * multi-byte values we need a `DataView` over the same buffer. Rather than allocating a new
 * `DataView` on every call, we cache one on the `Int8Array` instance itself under the unique
 * symbol `nakshaView`. The helper [viewOf] handles creation and caching transparently.
 *
 * JavaScript's `DataView` has no concept of a "native" byte order, so the no-suffix variants
 * (e.g. [getInt32]) fall back to **little-endian**, which matches the native byte order of all
 * common JS targets (x86 / ARM in LE mode).
 */
@JsExport
actual class ByteArrays {
    actual companion object ByteArraysCompanion {

        /** Symbol used as property key on `Int8Array` instances to cache the `DataView`. */
        private val nakshaViewSymbol: dynamic = js("Symbol('nakshaView')")

        /**
         * Returns a `DataView` over the underlying `ArrayBuffer` of [byteArray] (an `Int8Array`).
         * The view is created on first access and stored under [nakshaViewSymbol] for reuse.
         */
        private fun viewOf(byteArray: ByteArray): dynamic {
            val arr = byteArray.asDynamic()
            var view = arr[nakshaViewSymbol]
            if (view == null || view == undefined) {
                view = js("new DataView(arr.buffer, arr.byteOffset, arr.byteLength)")
                arr[nakshaViewSymbol] = view
            }
            return view
        }

        // ---- float32 ----------------------------------------------------------

        @JsStatic
        actual fun getFloat32(bytes: ByteArray, pos: Int): Float =
            viewOf(bytes).getFloat32(pos, true).unsafeCast<Float>()       // LE = native fallback

        @JsStatic
        actual fun getFloat32Be(bytes: ByteArray, pos: Int): Float =
            viewOf(bytes).getFloat32(pos, false).unsafeCast<Float>()

        @JsStatic
        actual fun getFloat32Le(bytes: ByteArray, pos: Int): Float =
            viewOf(bytes).getFloat32(pos, true).unsafeCast<Float>()

        @JsStatic
        actual fun setFloat32(bytes: ByteArray, pos: Int, value: Float) {
            viewOf(bytes).setFloat32(pos, value, true)
        }

        @JsStatic
        actual fun setFloat32Be(bytes: ByteArray, pos: Int, value: Float) {
            viewOf(bytes).setFloat32(pos, value, false)
        }

        @JsStatic
        actual fun setFloat32Le(bytes: ByteArray, pos: Int, value: Float) {
            viewOf(bytes).setFloat32(pos, value, true)
        }

        // ---- float64 ----------------------------------------------------------

        @JsStatic
        actual fun getFloat64(bytes: ByteArray, pos: Int): Double =
            viewOf(bytes).getFloat64(pos, true).unsafeCast<Double>()

        @JsStatic
        actual fun getFloat64Be(bytes: ByteArray, pos: Int): Double =
            viewOf(bytes).getFloat64(pos, false).unsafeCast<Double>()

        @JsStatic
        actual fun getFloat64Le(bytes: ByteArray, pos: Int): Double =
            viewOf(bytes).getFloat64(pos, true).unsafeCast<Double>()

        @JsStatic
        actual fun setFloat64(bytes: ByteArray, pos: Int, value: Double) {
            viewOf(bytes).setFloat64(pos, value, true)
        }

        @JsStatic
        actual fun setFloat64Be(bytes: ByteArray, pos: Int, value: Double) {
            viewOf(bytes).setFloat64(pos, value, false)
        }

        @JsStatic
        actual fun setFloat64Le(bytes: ByteArray, pos: Int, value: Double) {
            viewOf(bytes).setFloat64(pos, value, true)
        }

        // ---- int8 -------------------------------------------------------------

        @JsStatic
        actual fun getInt8(bytes: ByteArray, pos: Int): Byte =
            viewOf(bytes).getInt8(pos).unsafeCast<Byte>()

        @JsStatic
        actual fun setInt8(bytes: ByteArray, pos: Int, value: Byte) {
            viewOf(bytes).setInt8(pos, value)
        }

        // ---- int16 ------------------------------------------------------------

        @JsStatic
        actual fun getInt16(bytes: ByteArray, pos: Int): Short =
            viewOf(bytes).getInt16(pos, true).unsafeCast<Short>()

        @JsStatic
        actual fun getInt16Be(bytes: ByteArray, pos: Int): Short =
            viewOf(bytes).getInt16(pos, false).unsafeCast<Short>()

        @JsStatic
        actual fun getInt16Le(bytes: ByteArray, pos: Int): Short =
            viewOf(bytes).getInt16(pos, true).unsafeCast<Short>()

        @JsStatic
        actual fun setInt16(bytes: ByteArray, pos: Int, value: Short) {
            viewOf(bytes).setInt16(pos, value, true)
        }

        @JsStatic
        actual fun setInt16Be(bytes: ByteArray, pos: Int, value: Short) {
            viewOf(bytes).setInt16(pos, value, false)
        }

        @JsStatic
        actual fun setInt16Le(bytes: ByteArray, pos: Int, value: Short) {
            viewOf(bytes).setInt16(pos, value, true)
        }

        // ---- int32 ------------------------------------------------------------

        @JsStatic
        actual fun getInt32(bytes: ByteArray, pos: Int): Int =
            viewOf(bytes).getInt32(pos, true).unsafeCast<Int>()

        @JsStatic
        actual fun getInt32Be(bytes: ByteArray, pos: Int): Int =
            viewOf(bytes).getInt32(pos, false).unsafeCast<Int>()

        @JsStatic
        actual fun getInt32Le(bytes: ByteArray, pos: Int): Int =
            viewOf(bytes).getInt32(pos, true).unsafeCast<Int>()

        @JsStatic
        actual fun setInt32(bytes: ByteArray, pos: Int, value: Int) {
            viewOf(bytes).setInt32(pos, value, true)
        }

        @JsStatic
        actual fun setInt32Be(bytes: ByteArray, pos: Int, value: Int) {
            viewOf(bytes).setInt32(pos, value, false)
        }

        @JsStatic
        actual fun setInt32Le(bytes: ByteArray, pos: Int, value: Int) {
            viewOf(bytes).setInt32(pos, value, true)
        }

        // ---- int64 ------------------------------------------------------------

        @JsStatic
        actual fun getInt64(bytes: ByteArray, pos: Int): Long =
            viewOf(bytes).getBigInt64(pos, true).unsafeCast<Long>()

        @JsStatic
        actual fun getInt64Be(bytes: ByteArray, pos: Int): Long =
            viewOf(bytes).getBigInt64(pos, false).unsafeCast<Long>()

        @JsStatic
        actual fun getInt64Le(bytes: ByteArray, pos: Int): Long =
            viewOf(bytes).getBigInt64(pos, true).unsafeCast<Long>()

        @JsStatic
        actual fun setInt64(bytes: ByteArray, pos: Int, value: Long) {
            viewOf(bytes).setBigInt64(pos, value, true)
        }

        @JsStatic
        actual fun setInt64Be(bytes: ByteArray, pos: Int, value: Long) {
            viewOf(bytes).setBigInt64(pos, value, false)
        }

        @JsStatic
        actual fun setInt64Le(bytes: ByteArray, pos: Int, value: Long) {
            viewOf(bytes).setBigInt64(pos, value, true)
        }
    }
}
