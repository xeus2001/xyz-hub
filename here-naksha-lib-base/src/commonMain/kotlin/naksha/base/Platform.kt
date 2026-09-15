@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "OPT_IN_USAGE")

package naksha.base

import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * The platform abstraction, implemented for each platform to support the multi-platform code. All methods in this singleton are
 * by definition thread safe.
 */
expect class Platform {
    companion object PlatformCompanion {
        /**
         * The platform specific value of undefined.
         */
        val UNDEFINED: Any

        /**
         * The default symbol used for all proxies for which no explicit symbol is returned by the symbol resolvers.
         */
        val DEFAULT_SYMBOL: Symbol

        /**
         * The iterator member.
         */
        val ITERATOR: Symbol

        /**
         * The maximum value of a 64-bit integer.
         * @return The maximum value of a 64-bit integer.
         */
        val INT64_MAX_VALUE: Long

        /**
         * The minimum value of a 64-bit integer.
         * @return The minimum value of a 64-bit integer.
         */
        val INT64_MIN_VALUE: Long

        /**
         * The minimum integer that can safely stored in a double.
         * @return The minimum integer that can safely stored in a double.
         */
        val MAX_SAFE_INT: Double

        /**
         * The maximum integer that can safely stored in a double.
         * @return The maximum integer that can safely stored in a double.
         */
        val MIN_SAFE_INT: Double

        /**
         * The difference between 1 and the smallest floating point number greater than 1.
         */
        val EPSILON: Double

        /**
         * The KClass for [Any].
         */
        val anyKlass: KClass<Any>

        /**
         * The KClass for [Boolean].
         */
        val booleanKlass: KClass<Boolean>

        /**
         * The KClass for [Short].
         */
        val shortKlass: KClass<Short>

        /**
         * The KClass for [Int].
         */
        val intKlass: KClass<Int>

        /**
         * The KClass for [Long].
         */
        val int64Klass: KClass<Long>

        /**
         * The KClass for [Double].
         */
        val doubleKlass: KClass<Double>

        /**
         * The KClass for [String].
         */
        val stringKlass: KClass<String>

        /**
         * The KClass for [PlatformObject].
         */
        val objectKlass: KClass<PlatformObject>

        /**
         * The KClass for [PlatformList].
         */
        val listKlass: KClass<PlatformList>

        /**
         * The KClass for [PlatformMap].
         */
        val mapKlass: KClass<PlatformMap>

        /**
         * The KClass for [PlatformDataViewApi].
         */
        val dataViewKlass: KClass<PlatformDataView>

        /**
         * The [PlatformLogger].
         */
        val logger: PlatformLogger

        /**
         * Tests if the given value is _null_ or _undefined_.
         * @param any The value to test.
         * @return _true_ if the value is _null_ or _undefined_; false otherwise.
         */
        fun isNil(any: Any?): Boolean

        /**
         * Must be called ones in the lifetime of an application to initialize the multi-platform code. The method is thread safe and
         * only does something when first called.
         * @return _true_ if this was the first call and the platform was initialized; _false_ if the platform is already initialized.
         */
        fun initialize(): Boolean

        /**
         * Tests if the [target] class or interface is either the same as, or is a superclass or superinterface of, the class
         * or interface represented by the specified [source] parameter.
         *
         * For example `isAssignable(CharSequence, String)` would be _false_ (not every [CharSequence] is always a [String]),
         * while `isAssignable(String, CharSequence)` will be _true_ (every [String] is always a [CharSequence]).
         *
         * In other words, this method tests if the [source] type can be cast down to the [target] type, so if
         * **`source as target`** is possible.
         *
         * **Warning**: An assignment is not the same as an instanceof test. For example for interfaces the example can be tricky,
         * because formally the cast from a [CharSequence] to a [String] is not an assignable form, but technically can still
         * succeed, if the object being tried to cast down is actually a string, just the compiler type is formally [CharSequence].
         * Formally this kind of cast is an assignment from [String] to [String] not being known at compile time.
         *
         * @param source The type that should be cast.
         * @param target The target type to which to cast.
         * @return _true_ if the [source] type can be cast to the [target] type in all cases; _false_ otherwise.
         */
        fun isAssignable(source: KClass<*>, target: KClass<*>): Boolean

        /**
         * Tests if the given type is any [Proxy]. This is necessary to be used with [proxy].
         * @param klass The type to test.
         * @return _true_ if the given type is a [Proxy] type; _false_ otherwise.
         */
        fun isProxyKlass(klass: KClass<*>): Boolean

        /**
         * Returns the [KClass] **of** the given object.
         * @param o The object to query.
         * @return The [KClass] **of** the given object.
         * @throws IllegalArgumentException If the given object has no valid [KClass].
         */
        fun <T : Any> klassOf(o: T): KClass<T>

        /**
         * A reflective method to turn a full qualified classname into a klass instance.
         *
         * - In the JVM this method will use `Class.forName` to find the class, initialize it, and then return the Kotlin class of it.
         * - In JavaScript this method will use `globalThis` to find the constructor, so `com.here.example.Foo` will resolve into `globalThis.com.here.example.Foo`, this is expected to be a constructor function, then using [klassFor] to resolve it into the Kotlin class.
         * @param name the full qualified name of the Klass.
         * @return the Klass.
         * @since 3.0.0
         * @throws IllegalArgumentException if no such Klass is found.
         */
        fun <T : Any> klassForName(name: String): KClass<T>

        /**
         * Intern the given string and perform a [NFC](https://unicode.org/reports/tr15/) (Canonical Decomposition,
         * followed by Canonical Composition). Optionally, if [cd] is set to _true_, perform a Compatibility Decomposition,
         * followed by Canonical Composition. Beware that this is only good for search or special cases, the recommended
         * form is NFC (the default).
         * @param s The string to intern and convert to NFC form.
         * @param cd If _true_, then perform a Compatibility Decomposition instead of the Canonical Decomposition.
         * @return The new interned string or the given one, if it is already in the right form.
         */
        fun intern(s: String, cd: Boolean = false): String

        /**
         * Creates a new array.
         * @param entries The entries to initialize the array with.
         * @return The created array.
         */
        fun newList(vararg entries: Any?): PlatformList

        /**
         * Creates a new map.
         * @param entries The entries to add into the map. Can be a list of [Pair] or alternating (`key`, `value`)'s.
         * @return The created map.
         */
        fun newMap(vararg entries: Any?): PlatformMap

        /**
         * Create a new concurrent map.
         * @return The concurrent map.
         */
        fun <K : Any, V : Any> newAtomicMap(): AtomicMap<K, V>

        /**
         * Create a new atomic reference.
         * @param startValue the initial value.
         * @return the atomic reference.
         */
        fun <R: Any> newAtomicRef(startValue: R?): AtomicRef<R>

        /**
         * Create a new atomic reference that is never `null`.
         * @param startValue the initial value must not `null`.
         * @return the atomic reference.
         */
        fun <R: Any> newAtomicNonNullRef(startValue: R): AtomicNonNullRef<R>

        /**
         * Create a new atomic boolean.
         * @param startValue the initial value.
         * @return the atomic boolean.
         */
        fun newAtomicBool(startValue: Boolean): AtomicBool

        /**
         * Create a new atomic integer.
         * @param startValue the initial value.
         * @return the atomic integer.
         */
        fun newAtomicInt(startValue: Int): AtomicInt

        /**
         * Create a new atomic 64-bytes based integer (long).
         * @param startValue the initial value.
         * @return the atomic integer.
         */
        fun newAtomicInt64(startValue: Long): AtomicInt64

        /**
         * Creates a new byte-array of the given size.
         * @param size The size in byte.
         * @return The byte-array of the given size.
         */
        fun newByteArray(size: Int): ByteArray

        /**
         * Creates a view above a byte-array to access the content.
         * @param byteArray The array to map.
         * @param offset The offset of the first byte to map, defaults to 0.
         * @param size The amount of byte to map.
         * @return The data view.
         * @throws IllegalArgumentException If any of the given arguments is invalid.
         */
        fun newDataView(byteArray: ByteArray, offset: Int = 0, size: Int = byteArray.size - offset): PlatformDataView

        /**
         * Creates a new thread-local. Should be stored only in a static immutable variable (`val`).
         * @param initializer An optional lambda to be invoked, when the thread-local is read for the first time.
         * @return The thread local.
         */
        fun <T> newThreadLocal(initializer: (() -> T)? = null): PlatformThreadLocal<T>

        /**
         * Creates a new weak reference to the given referent.
         * @param referent the referent.
         * @return the weak reference to the referent.
         */
        fun <T : Any> newWeakRef(referent: T): WeakRef<T>

        /**
         * Creates a new reentrant lock.
         * @return the created reentrant lock.
         */
        fun newLock(): PlatformLock

        /**
         * Create a proxy or return the existing proxy. If a proxy of a not compatible type exists already and [doNotOverride] is _true_,
         * the method will throw an _IllegalStateException_; otherwise the current type is simply overridden.
         * @param pobject the object at which to query for the proxy ([PlatformMap], [PlatformList] or [PlatformDataView]).
         * @param klass the proxy class.
         * @param doNotOverride if _true_, do not override existing symbols bound to incompatible types, but throw an
         * [IllegalStateException].
         * @return the proxy instance.
         * @throws IllegalArgumentException if the given `object` is not [PlatformMap], [PlatformList] or [PlatformDataView].
         * @throws IllegalStateException if [doNotOverride] is _true_ and the symbol is already bound to an incompatible type.
         */
        fun <T : Proxy> proxy(pobject: PlatformObject, klass: KClass<T>, doNotOverride: Boolean = false): T

        /**
         * Return the native platform object for the given proxy. If the given object is no proxy, the object is returned
         * as given.
         * @param value The object to access.
         * @return The [PlatformObject] if a [Proxy] given; otherwise the value itself.
         */
        fun unbox(value: Any?): Any?

        /**
         * Create a 32-bit integer from the given value.
         * @param value A value being either [Number] or [String] that contains a decimal number.
         * @return The value as 32-bit integer.
         * @throws IllegalArgumentException If the given value fails to be converted into a 32-bit integer.
         */
        fun toInt(value: Any): Int

        /**
         * Create a 64-bit integer from the given value.
         * @param value A value being either a [Number] or a [String] that contains a decimal number.
         * @return The value as 64-bit integer.
         * @throws IllegalArgumentException If the given value fails to be converted into a 64-bit integer.
         */
        fun toInt64(value: Any): Long

        /**
         * Create a 64-bit floating point number from the given value.
         * @param value A value being either [Number] or [String].
         * @return The value as 64-bit floating point number.
         * @throws IllegalArgumentException If the given value fails to be converted into a 64-bit floating point number.
         */
        fun toDouble(value: Any): Double

        /**
         * Cast the given 64-bit integer into a 64-bit floating point number using only raw bits. That means, the 64-bits of the
         * integer are treated as if they store an [IEEE-754](https://en.wikipedia.org/wiki/IEEE_754) 64-bit floating point number,
         * so for example 0xffff_ffff_ffff_ffff becomes [Double.NaN].
         * @param i The 64-bit integer.
         * @return The integer converted into a double.
         */
        fun toDoubleRawBits(i: Long): Double

        /**
         * Cast the given 64-bit floating point number into a 64-bit integer using only raw bits. That means, the 64-bits of the
         * floating point are treated as if they are simply a 64-bit integer, so for example [Double.NaN] becomes 0xffff_ffff_ffff_ffff.
         * @param d The 64-bit double in [IEEE-754](https://en.wikipedia.org/wiki/IEEE_754) format.
         * @return The integer converted into a double.
         */
        fun toInt64RawBits(d: Double): Long

        /**
         * Widen a 32-bit integer into a platform specific 64-bit integer.
         * @param value the 32-bit integer.
         * @return the platform specific 64-bit representation.
         */
        fun intToInt64(value: Int): Long

        /**
         * Converts an internal 64-bit integer into a platform specific.
         * @param value The internal 64-bit.
         * @return The platform specific 64-bit.
         */
        fun longToInt64(value: Long): Long

        /**
         * Converts a platform specific 64-bit integer into an internal one to be used for example with the [PlatformDataViewApi].
         * @param value The platform specific 64-bit integer.
         * @return The internal 64-bit integer.
         */
        fun int64ToLong(value: Long): Long

        /**
         * Tests if the given object is a scalar, so _null_, _undefined_, any [Number], [String] or [Boolean].
         * @param o The object to test.
         * @return _true_ if the object is a scalar; _false_ otherwise.
         */
        fun isScalar(o: Any?): Boolean

        /**
         * Tests if the given object is a [Number].
         * @param o The object to test.
         * @return _true_ if the object is a [Number]; _false_ otherwise.
         */
        fun isNumber(o: Any?): Boolean

        /**
         * Tests if the given object is a [Byte], [Short], [Int] or [Long].
         * @param o The object to test.
         * @return _true_ if the object is a [Byte], [Short], [Int] or [Long]; _false_ otherwise.
         */
        fun isInteger(o: Any?): Boolean

        /**
         * Tests if the given object is a [Double].
         * @param o The object to test.
         * @return _true_ if the object is a [Double]; _false_ otherwise.
         */
        fun isDouble(o: Any?): Boolean

        /**
         * Compare the two given objects. If object a support the "compareTo" method it is invoked, if not or comparing fails with an
         * exception, "compareTo" of b is tried and eventually the hashCode of both object is calculated and compared.
         * @param a The first object to compare.
         * @param b The second object to compare.
         * @return -1 if the [a] is less than [b]; 0 if they are equal; 1 if [a] is greater than [b].
         */
        fun compare(a: Any?, b: Any?): Int

        /**
         * Calculate a hash-code of the given value and return it. At the JVM this will always just call [hashCode] of the given object,
         * in JavaScript it will try if there is a "hashCode" method on the object and then call, otherwise some default alternative,
         * which will be a stringify with FNV1a hash and then storing the hash in a symbol to have same behavior as in Java.
         * @param o The value to calculate the hash-code.
         * @return The 32-bit hash code.
         */
        fun hashCodeOf(o: Any?): Int

        /**
         * Creates a new initialized instance of the given type, using the parameterless constructor.
         * @param klass The type of which to create a new instance.
         * @return The new instance.
         * @throws IllegalArgumentException If there is no parameterless constructor.
         */
        fun <T : Any> newInstanceOf(klass: KClass<out T>): T

        /**
         * Creates a new instance of the given type, bypassing the constructor, so it returns the uninitialized class.
         * @param klass The type of which to create a new instance.
         * @return The new instance.
         */
        fun <T : Any> allocateInstance(klass: KClass<out T>): T

        /**
         * Forces the class loader to initialize the given Kotlin class.
         * @param klass The type to initialize.
         */
        fun initializeKlass(klass: KClass<*>)

        /**
         * Ask the platform to make a copy of the given platform object.
         *
         * This method supports copy of:
         * - [PlatformMap]
         * - [PlatformList]
         * - [PlatformDataView]
         * - [Boolean]
         * - [Short]
         * - [Int]
         * - [Long]
         * - [Float]
         * - [Double]
         * - [String]
         *
         * No other types are supported.
         *
         * @param obj the object to make a copy of.
         * @param recursive _true_ if the copy should be made recursive; _false_ if a shallow copy should be made.
         * @return the copy.
         */
        fun <T> copy(obj: T?, recursive: Boolean) : T?

        /**
         * Serialize the given value to JSON.
         * @param obj the object to serialize.
         * @return the JSON string.
         * @see [ToJsonOptions.DEFAULT]
         */
        fun toJSON(obj: Any?): String

        /**
         * Serialize the given value to JSON.
         * @param obj the object to serialize.
         * @param options the options to use; defaults to [ToJsonOptions.DEFAULT].
         * @return the JSON string.
         */
        fun toJSON(obj: Any?, options: ToJsonOptions = ToJsonOptions.DEFAULT): String

        /**
         * Deserialize the given JSON.
         * @param json The JSON string to parse.
         * @return The parsed JSON.
         * @see [FromJsonOptions.DEFAULT]
         */
        fun fromJSON(json: String): Any?

        /**
         * Deserialize the given JSON.
         * @param json The JSON UTF-8 encoded byte array to parse.
         * @return The parsed JSON.
         * @see [FromJsonOptions.DEFAULT]
         */
        fun fromJSON(utf8: ByteArray): Any?

        /**
         * Deserialize the given JSON.
         * @param json the JSON string to parse.
         * @param options the options to use; defaults to [FromJsonOptions.DEFAULT].
         * @return The parsed JSON.
         */
        fun fromJSON(json: String, options: FromJsonOptions = FromJsonOptions.DEFAULT): Any?

        /**
         * Convert the given platform native objects recursively into multi-platform objects. So all maps are corrected to [PlatformMap],
         * all strings starting with `data:bigint,` are converted into [Long]'s, lists are corrected to [PlatformList],
         * and so on. This can be used after a JSON was parsed from an arbitrary platform tool into some platform specific standard
         * objects or when exchanging data with a platform specific library that does not like the multi-platform objects.
         * @param obj The platform native objects to convert recursively.
         * @param importers The importers to use.
         * @return The given platform native objects converted into multi-platform objects.
         */
        fun fromPlatform(obj: Any?, importers: List<PlatformImporter>): Any?

        /**
         * Convert the given multi-platform objects recursively into the default platform native objects, for example [PlatformMap] may
         * become a pure `Object` in JavaScript. This is often useful when exchanging code with libraries that do not support `Map`.
         * In Java this will convert to [PlatformMap] to [LinkedHashMap].
         * @param obj The multi-platform objects to be converted into platform native objects.
         * @param exporters The exporters to use.
         * @return The platform native objects.
         */
        fun toPlatform(obj: Any?, exporters: List<PlatformExporter>): Any?

        /**
         * Returns the current epoch milliseconds.
         * @return The current epoch milliseconds.
         */
        fun currentMillis(): Long

        /**
         * Returns the current epoch microseconds.
         * @return current epoch microseconds.
         */
        fun currentMicros(): Long

        /**
         * Returns the current epoch nanoseconds.
         * @return current epoch nanoseconds.
         */
        fun currentNanos(): Long

        /**
         * Generates a new random number between 0 and 1 (therefore with 53-bit random bits).
         * @return The new random number between 0 and 1.
         */
        fun random(): Double

        /**
         * Tests if the given 64-bit floating point number can be converted into a 32-bit floating point number without losing information.
         * @param value The 64-bit floating point number.
         * @return _true_ if the given 64-bit float can be converted into a 32-bit one without losing information; _false_ otherwise.
         */
        fun canBeFloat32(value: Double): Boolean

        /**
         * Tests if the given 64-bit floating point number can be converted into a 32-bit integer without losing information.
         * @param value The 64-bit floating point number.
         * @return _true_ if the given 64-bit float can be converted into a 32-bit integer without losing information; _false_ otherwise.
         */
        fun canBeInt32(value: Double): Boolean

        /**
         * Tests if this code is executed within a PostgresQL database using [PLV8 extension](https://plv8.github.io/).
         * @return _true_ if this code is executed within PostgresQL database using [PLV8 extension](https://plv8.github.io/).
         */
        fun isPlv8(): Boolean

        /**
         * The `encodeURIComponent()` function encodes a URI by replacing each instance of certain characters by one, two, three, or four escape sequences representing the UTF-8 encoding of the character (will only be four escape sequences for characters composed of two surrogate characters).
         * @param uriComponent a string to be encoded as a URI component (a path, query string, fragment, etc.).
         * @return a new string representing the provided uriComponent encoded as a URI component.
         * @since 3.0
         */
        fun encodeURIComponent(uriComponent: String): String

        /**
         * The `decodeURIComponent()` function decodes a Uniform Resource Identifier (URI) component previously created by [encodeURIComponent] or by a similar routine.
         * @param encodedURI an encoded component of a Uniform Resource Identifier.
         * @return a new string representing the decoded version of the given encoded Uniform Resource Identifier (URI) component.
         * @since 3.0
         */
        fun decodeURIComponent(encodedURI: String): String

        /**
         * Calculates the MD5 hash about the given text.
         *
         * @param text the text to hash.
         * @return the MD5 hash, being a byte-array with size 16 (128-bit).
         */
        fun md5(text: String): ByteArray

        /**
         * Calculates the MD5 hash about the given byte-array.
         *
         * @param bytes the byte-array to hash.
         * @return the MD5 hash, being a byte-array with size 16 (128-bit).
         */
        fun md5(bytes: ByteArray): ByteArray

        /**
         * Compress bytes.
         * @param raw the bytes to compress.
         * @return the deflated (compressed) bytes.
         */
        fun lz4Deflate(raw: ByteArray): ByteArray

        /**
         * Decompress bytes.
         * @param compressed the compressed bytes.
         * @return the inflated (decompress) bytes.
         */
        fun lz4Inflate(compressed: ByteArray): ByteArray

        /**
         * Compress bytes.
         * @param raw the bytes to compress.
         * @return the deflated (compressed) bytes.
         */
        fun gzipDeflate(raw: ByteArray): ByteArray

        /**
         * Decompress bytes.
         * @param compressed the bytes to decompress.
         * @return the inflated (decompress) bytes.
         */
        fun gzipInflate(compressed: ByteArray): ByteArray

        /**
         * Create a stack-trace as string for debugging purpose.
         *
         * In Kotlin, you can simply invoke [Throwable.stackTraceToString], which is how this method is implemented.
         * @param t the throwable for which to return the stack-trace.
         * @return the stack-trace as string.
         */
        fun stackTrace(t: Throwable): String

        /**
         * Normalize the string using selected [form](https://www.unicode.org/reports/tr15/#Norm_Forms)
         *
         * @param value - string value to normalize
         * @param form - [NormalizerForm] to use
         * @return new normalized string
         */
        fun normalize(value: String, form: NormalizerForm): String

        /**
         * Returns the identifier of the test storage, either read from environment variable `NAKSHA_TEST_STORAGE_ID` or a default value.
         * @return the identifier of the test storage.
         * @since 3.0
         */
        fun getTestStorageId(): String

        /**
         * `File-And-Line` - Returns the filename and code line of the caller as string: `[filename:line] `. If not supported on the target platform, returns an empty string.
         *
         * Can be added to the start of an exception like:
         * ```
         * throw illegalArg("${FAL}Some message")
         * ```
         * Resulting in an effective string:
         * ```
         * [Foo.kt:123] Some message
         * ```
         * This is very helpful for debugging, if not supported on a platform, it becomes just:
         * ```
         * Some message
         * ```
         * @return the filename and code line of the caller as string or an empty string.
         * @since 3.0
         */
        val FAL: String

        /**
         * `File-And-Line` - Returns the filename and code line of the caller as string: `[filename:line] `. If not supported on the target platform, returns an empty string.
         *
         * Can be added to the start of an exception like:
         * ```
         * throw illegalArg("${fal(3)}Some message")
         * ```
         * Resulting in an effective string:
         * ```
         * [Foo.kt:123] Some message
         * ```
         * This is very helpful for debugging, if not supported on a platform, it becomes just:
         * ```
         * Some message
         * ```
         * @param n the amount of frames to go back, minimally `1` which is the caller.
         * @return the filename and code line of the caller as string or an empty string.
         * @since 3.0
         */
        fun fal(n: Int): String
    }
}
