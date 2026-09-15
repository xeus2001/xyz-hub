@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package naksha.base

import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.nio.ByteOrder

/**
 * JVM actual implementation of [ByteArrays].
 *
 * All multi-byte access is performed via [VarHandle] instances obtained from
 * [MethodHandles.byteArrayViewVarHandle], which is the modern, safe alternative to `sun.misc.Unsafe`
 * and produces optimal JIT-compiled code on all major JVM implementations.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class ByteArrays {
    actual companion object ByteArraysCompanion {

        // ---- VarHandles -------------------------------------------------------

        private val VH_FLOAT_NAT: VarHandle =
            MethodHandles.byteArrayViewVarHandle(FloatArray::class.java, ByteOrder.nativeOrder())
        private val VH_FLOAT_BE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(FloatArray::class.java, ByteOrder.BIG_ENDIAN)
        private val VH_FLOAT_LE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(FloatArray::class.java, ByteOrder.LITTLE_ENDIAN)

        private val VH_DOUBLE_NAT: VarHandle =
            MethodHandles.byteArrayViewVarHandle(DoubleArray::class.java, ByteOrder.nativeOrder())
        private val VH_DOUBLE_BE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(DoubleArray::class.java, ByteOrder.BIG_ENDIAN)
        private val VH_DOUBLE_LE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(DoubleArray::class.java, ByteOrder.LITTLE_ENDIAN)

        private val VH_SHORT_NAT: VarHandle =
            MethodHandles.byteArrayViewVarHandle(ShortArray::class.java, ByteOrder.nativeOrder())
        private val VH_SHORT_BE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(ShortArray::class.java, ByteOrder.BIG_ENDIAN)
        private val VH_SHORT_LE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(ShortArray::class.java, ByteOrder.LITTLE_ENDIAN)

        private val VH_INT_NAT: VarHandle =
            MethodHandles.byteArrayViewVarHandle(IntArray::class.java, ByteOrder.nativeOrder())
        private val VH_INT_BE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(IntArray::class.java, ByteOrder.BIG_ENDIAN)
        private val VH_INT_LE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(IntArray::class.java, ByteOrder.LITTLE_ENDIAN)

        private val VH_LONG_NAT: VarHandle =
            MethodHandles.byteArrayViewVarHandle(LongArray::class.java, ByteOrder.nativeOrder())
        private val VH_LONG_BE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(LongArray::class.java, ByteOrder.BIG_ENDIAN)
        private val VH_LONG_LE: VarHandle =
            MethodHandles.byteArrayViewVarHandle(LongArray::class.java, ByteOrder.LITTLE_ENDIAN)

        // ---- float32 ----------------------------------------------------------

        @JvmStatic
        actual fun getFloat32(bytes: ByteArray, pos: Int): Float = VH_FLOAT_NAT.get(bytes, pos) as Float
        @JvmStatic
        actual fun getFloat32Be(bytes: ByteArray, pos: Int): Float = VH_FLOAT_BE.get(bytes, pos) as Float
        @JvmStatic
        actual fun getFloat32Le(bytes: ByteArray, pos: Int): Float = VH_FLOAT_LE.get(bytes, pos) as Float

        @JvmStatic
        actual fun setFloat32(bytes: ByteArray, pos: Int, value: Float) { VH_FLOAT_NAT.set(bytes, pos, value) }
        @JvmStatic
        actual fun setFloat32Be(bytes: ByteArray, pos: Int, value: Float) { VH_FLOAT_BE.set(bytes, pos, value) }
        @JvmStatic
        actual fun setFloat32Le(bytes: ByteArray, pos: Int, value: Float) { VH_FLOAT_LE.set(bytes, pos, value) }

        // ---- float64 ----------------------------------------------------------

        @JvmStatic
        actual fun getFloat64(bytes: ByteArray, pos: Int): Double = VH_DOUBLE_NAT.get(bytes, pos) as Double
        @JvmStatic
        actual fun getFloat64Be(bytes: ByteArray, pos: Int): Double = VH_DOUBLE_BE.get(bytes, pos) as Double
        @JvmStatic
        actual fun getFloat64Le(bytes: ByteArray, pos: Int): Double = VH_DOUBLE_LE.get(bytes, pos) as Double

        @JvmStatic
        actual fun setFloat64(bytes: ByteArray, pos: Int, value: Double) { VH_DOUBLE_NAT.set(bytes, pos, value) }
        @JvmStatic
        actual fun setFloat64Be(bytes: ByteArray, pos: Int, value: Double) { VH_DOUBLE_BE.set(bytes, pos, value) }
        @JvmStatic
        actual fun setFloat64Le(bytes: ByteArray, pos: Int, value: Double) { VH_DOUBLE_LE.set(bytes, pos, value) }

        // ---- int8 -------------------------------------------------------------

        @JvmStatic
        actual fun getInt8(bytes: ByteArray, pos: Int): Byte = bytes[pos]
        @JvmStatic
        actual fun setInt8(bytes: ByteArray, pos: Int, value: Byte) { bytes[pos] = value }

        // ---- int16 ------------------------------------------------------------

        @JvmStatic
        actual fun getInt16(bytes: ByteArray, pos: Int): Short = VH_SHORT_NAT.get(bytes, pos) as Short
        @JvmStatic
        actual fun getInt16Be(bytes: ByteArray, pos: Int): Short = VH_SHORT_BE.get(bytes, pos) as Short
        @JvmStatic
        actual fun getInt16Le(bytes: ByteArray, pos: Int): Short = VH_SHORT_LE.get(bytes, pos) as Short

        @JvmStatic
        actual fun setInt16(bytes: ByteArray, pos: Int, value: Short) { VH_SHORT_NAT.set(bytes, pos, value) }
        @JvmStatic
        actual fun setInt16Be(bytes: ByteArray, pos: Int, value: Short) { VH_SHORT_BE.set(bytes, pos, value) }
        @JvmStatic
        actual fun setInt16Le(bytes: ByteArray, pos: Int, value: Short) { VH_SHORT_LE.set(bytes, pos, value) }

        // ---- int32 ------------------------------------------------------------

        @JvmStatic
        actual fun getInt32(bytes: ByteArray, pos: Int): Int = VH_INT_NAT.get(bytes, pos) as Int
        @JvmStatic
        actual fun getInt32Be(bytes: ByteArray, pos: Int): Int = VH_INT_BE.get(bytes, pos) as Int
        @JvmStatic
        actual fun getInt32Le(bytes: ByteArray, pos: Int): Int = VH_INT_LE.get(bytes, pos) as Int

        @JvmStatic
        actual fun setInt32(bytes: ByteArray, pos: Int, value: Int) { VH_INT_NAT.set(bytes, pos, value) }
        @JvmStatic
        actual fun setInt32Be(bytes: ByteArray, pos: Int, value: Int) { VH_INT_BE.set(bytes, pos, value) }
        @JvmStatic
        actual fun setInt32Le(bytes: ByteArray, pos: Int, value: Int) { VH_INT_LE.set(bytes, pos, value) }

        // ---- int64 ------------------------------------------------------------

        @JvmStatic
        actual fun getInt64(bytes: ByteArray, pos: Int): Long = VH_LONG_NAT.get(bytes, pos) as Long
        @JvmStatic
        actual fun getInt64Be(bytes: ByteArray, pos: Int): Long = VH_LONG_BE.get(bytes, pos) as Long
        @JvmStatic
        actual fun getInt64Le(bytes: ByteArray, pos: Int): Long = VH_LONG_LE.get(bytes, pos) as Long

        @JvmStatic
        actual fun setInt64(bytes: ByteArray, pos: Int, value: Long) {
            VH_LONG_NAT.set(bytes, pos, value)
        }
        @JvmStatic
        actual fun setInt64Be(bytes: ByteArray, pos: Int, value: Long) {
            VH_LONG_BE.set(bytes, pos, value)
        }
        @JvmStatic
        actual fun setInt64Le(bytes: ByteArray, pos: Int, value: Long) {
            VH_LONG_LE.set(bytes, pos, value)
        }
    }
}
