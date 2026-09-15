package naksha.base

import naksha.base.Platform.PlatformCompanion.random
import kotlin.js.JsExport
import kotlin.js.JsStatic
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.text.toInt

/**
 * All utility functions that work cross-platform.
 */
@Suppress("OPT_IN_USAGE", "unused")
@JsExport
class PlatformUtil {
    companion object PlatformUtilCompanion {
        /**
         * A switch to toggle debug logs (disabled by default).
         *
         * Sometimes exceptions are caught internally in `lib-psql`, sometimes even suppressed, this normally no problem, but when debugging this can become a problem, therefore this switch enables to print suppressed stack traces as INFO logs and other debug information. It may as well send
         */
        @JsStatic
        @JvmField
        var ENABLE_DEBUG: Boolean = false

        /**
         * A switch to toggle info logs (disabled by default).
         */
        @JsStatic
        @JvmField
        var ENABLE_INFO: Boolean = false

        /**
         * A switch to toggle warning logs (enabled by default).
         */
        @JsStatic
        @JvmField
        var ENABLE_WARN: Boolean = true

        /**
         * A switch to toggle error logs (enabled by default).
         */
        @JsStatic
        @JvmField
        var ENABLE_ERROR: Boolean = true

        /**
         * The maximal 32-bit floating point number that can be encoded without losing precision.
         */
        @JsStatic
        @JvmField
        val FLOAT_MAX: Double = Double.fromBits(0x47efffffe0000000L)

        /**
         * The minimal 32-bit floating point number that can be encoded without losing precision.
         */
        @JsStatic
        @JvmField
        val FLOAT_MIN: Double = Double.fromBits(0x36a0000000000000L)

        /**
         * A single milliseconds.
         */
        @JsStatic
        @JvmField
        val MILLISECOND = 1L

        /**
         * A second in milliseconds.
         */
        @JsStatic
        @JvmField
        val SECOND = 1000L

        /**
         * A minute in milliseconds.
         */
        @JsStatic
        @JvmField
        val MINUTE = 60 * 1000L

        /**
         * An hour in milliseconds.
         */
        @JsStatic
        @JvmField
        val HOUR = 60 * 60 * 1000L

        /**
         * A day in milliseconds.
         */
        @JsStatic
        @JvmField
        val DAY = 24 * 60 * 60 * 1000L

        /**
         * A multiplier to convert milliseconds to microseconds or a divider, to turn microseconds into millis.
         */
        @JsStatic
        @JvmField
        val MILLIS_TO_MICROS = 1000L

        /**
         * The default size of a view. This is used at various placed.
         */
        @JsStatic
        @JvmField
        @Deprecated("DataView is deprecated")
        var defaultDataViewSize = 128

        /**
         * An array with the Web-Safe Base-64 characters.
         * @since 3.0
         */
        @JvmStatic
        private val randomCharacters = CharArray(64) {
            when (it) {
                in 0..9 -> ('0'.code + it).toChar()
                in 10..35 -> ('a'.code + (it - 10)).toChar()
                in 36..61 -> ('A'.code + (it - 36)).toChar()
                // This duplicates a and z, but we for random strings we do not care that much
                62 -> 'a'
                63 -> 'z'
                else -> throw IllegalStateException()
            }
        }

        /**
         * Generates a random string that Web-URL safe and matches those of the Web-Safe Base64 encoding, so persists
         * only out of `a` to `z`, `A` to `Z`, `0` to `9`.
         * @param length The amount of characters to return, if less than or equal zero, 12 characters are used.
         * @return The random string.
         * @see randomBase64String
         * @see randomAtoZ
         */
        @JvmStatic
        @JsStatic
        @Deprecated("This is not a balanced randomness", replaceWith = ReplaceWith("randomAtoZ()"))
        fun randomString(length: Int = 12): String {
            // This way, in JavaScript, we catch undefined.
            val end = if (length >= 1) length else 12
            val chars = randomCharacters
            val sb = StringBuilder()
            var pos = 0
            var i = (random() * 64.0).toInt()
            // The first character should not be 0 to 9!
            if (i < 10) i += 10
            sb.append(chars[i and 63])
            while (++pos < end) {
                i = (random() * 64.0).toInt()
                sb.append(chars[i and 63])
            }
            return sb.toString()
        }

        /**
         * An array with the Web-Safe Base-64 characters, `A-Za-z0-9-_`.
         * @since 3.0
         */
        private val base64Characters = CharArray(64) {
            when (it) {
                in 0..25 -> ('A'.code + (it - 0)).toChar()
                in 26..51 -> ('a'.code + (it - 26)).toChar()
                in 52..61 -> ('0'.code + (it - 52)).toChar()
                // This duplicates a and z, but we for random strings we do not care that much
                62 -> '-'
                63 -> '_'
                else -> throw internalError("Compiler error")
            }
        }

        /**
         * An array with characters from `a` to `z`.
         * @since 3.0
         */
        private val aToZ = CharArray(26) { ('a'.code + it).toChar() }

        /**
         * Generates a random string only out of `a` to `z` characters.
         * @param length The amount of characters to return, if less than or equal zero, 12 characters are used.
         * @return The random string.
         * @since 3.0
         */
        @JvmStatic
        @JsStatic
        fun randomBase64String(length: Int = 12): String {
            // This way, in JavaScript, we catch undefined.
            val end = if (length >= 1) length else 12
            val chars = base64Characters
            val sb = StringBuilder()
            var pos = 0
            var i = (random() * chars.size).toInt()
            // The first character should not be '0' to '9', '-' or '_'!
            while (i >= 52) i = (random() * chars.size).toInt()
            sb.append(chars[i and 63])
            while (++pos < end) {
                i = (random() * chars.size).toInt()
                sb.append(chars[i and 63])
            }
            return sb.toString()
        }

        /**
         * Generates a random string persisting only out of `a` to `z` characters.
         *
         * For the default length of 12 characters, there are ninety-five quadrillion _(95,428,956,661,682,200)_ possible combinations.
         * @param length The amount of characters to return, if less than or equal zero, 12 characters are used.
         * @return The random string.
         * @since 3.0
         */
        @JvmStatic
        @JsStatic
        @JvmOverloads
        fun randomAtoZ(length: Int = 12): String {
            // This way, in JavaScript, we catch undefined.
            val end = if (length >= 1) length else 12
            val chars = aToZ
            val sb = StringBuilder()
            for (pos in 0 until end) {
                val i = (random() * chars.size).toInt() % chars.size
                require(i in chars.indices) { "Invalid value in random string: $i" }
                sb.append(chars[i])
            }
            return sb.toString()
        }

        /**
         * Calculates a hash code above all given values.
         * @param values the value above which to calculate a hash-code.
         * @return the calculated hash-code.
         */
        @JvmStatic
        @JsStatic
        fun hashCodeOf(vararg values: Any?): Int = hashCodeOf(0, *values)

        private fun hashCodeOf(hashInput: Int, vararg values: Any?): Int {
            var hash = hashInput
            for (v in values) {
                when (v) {
                    null -> hash *= 31
                    is ByteArray -> hash = hash * 31 + v.contentHashCode()
                    is Array<*> -> for (e in v) hash = hashCodeOf(e)
                    is List<*> -> for (e in v) hash = hashCodeOf(e)
                    is Map<*,*> -> for (e in v) hash = hashCodeOf(e.value)
                    else -> hash = hash * 31 + v.hashCode()
                }
            }
            return hash
        }

        /**
         * Recursively compare this object with another, checking for values instead of just referential.
         * This is needed because for arrays, the == operation compares whether the arrays are the same object.
         * This will work for any nested structures of maps, lists, and arrays.
         */
        @JvmStatic
        @JsStatic
        fun deepEquals(obj1: Any?, obj2: Any?): Boolean {
            if (obj1 === obj2) return true  // Same reference, or both null
            if (obj1 == null || obj2 == null) return false  // One is null, the other is not
            if (obj1::class != obj2::class) return false  // Different types

            return when (obj1) {
                is Array<*> -> obj1.contentDeepEquals(obj2 as Array<*>)
                is List<*> -> obj1.size == (obj2 as List<*>).size && obj1.indices.all { index -> deepEquals(obj1[index], obj2[index]) }
                is Map<*, *> -> obj1.size == (obj2 as Map<*,*>).size && obj1.all { (k, v) -> deepEquals(v, obj2[k]) }
                else -> obj1 == obj2  // Primitive types, or any other objects
            }
        }
    }
}
