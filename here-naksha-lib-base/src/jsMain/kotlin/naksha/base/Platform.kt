@file:Suppress("OPT_IN_USAGE", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package naksha.base

import kotlin.math.round
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.createInstance

@JsExport
actual class Platform {
    actual companion object PlatformCompanion {
        private var isInitialized: Boolean = false

        internal val U64_MAX_VALUE = js("BigInt.asUintN(64,BigInt('18446744073709551615'))").unsafeCast<Int64>()
        internal val I64_MAX_VALUE = js("BigInt.asIntN(64,BigInt('9223372036854775807'))").unsafeCast<Int64>()
        internal val I64_MIN_VALUE = js("BigInt.asIntN(64,BigInt('-9223372036854775808'))").unsafeCast<Int64>()
        internal val I64_ZERO = js("BigInt.asIntN(64,BigInt(0))").unsafeCast<Int64>()
        internal val I64_ONE = js("BigInt.asIntN(64, BigInt(1))").unsafeCast<Int64>()
        internal val I64_MINUS_ONE = js("BigInt.asIntN(64,BigInt(-1))").unsafeCast<Int64>()
        internal val I64_BYTE_MASK = js("BigInt.asUintN(64,BigInt('0xff'))").unsafeCast<Int64>()
        internal val I64_SHORT_MASK = js("BigInt.asUintN(64,BigInt('0xffff'))").unsafeCast<Int64>()
        internal val I64_INT_MASK = js("BigInt.asUintN(64,BigInt('0xffffffff'))").unsafeCast<Int64>()
        internal val I64_INT64_MASK = js("BigInt.asUintN(64,BigInt('0xffffffffffffffff'))").unsafeCast<Int64>()
        internal val I64_TWO_COMPLEMENT_32 = js("BigInt.asUintN(64,BigInt('4294967296'))").unsafeCast<Int64>()
        internal val I64_TWO_COMPLEMENT_64 = js("BigInt.asUintN(64,BigInt('18446744073709551616'))").unsafeCast<Int64>()

        /**
         * Ensures that the given value is a real 64-bit integer.
         * @param number the value being any [Number] or a [Int64].
         * @return the value as real [Int64].
         */
        internal fun _int64(number: dynamic): Int64 {
            require(number !== undefined) { "Illegal Int64: undefined" }
            require(number !== null) { "Illegal Int64: null" }
            val v = number.valueOf()
            val type = jsTypeOf(v)
            if (type == "bigint") return v.unsafeCast<Int64>()
            if (type == "number") {
                if (js("Number.isInteger(v)").unsafeCast<Boolean>()) {
                    return js("BigInt.asIntN(64,BigInt(v))").unsafeCast<Int64>()
                }
                return js("BigInt.asIntN(64,BigInt(Math.round(v)))").unsafeCast<Int64>()
            }
            throw IllegalArgumentException("Illegal Int64: $number::$type")
        }

        /**
         * An array of 16 64-bit signed integers.
         */
        internal val i64_arr: dynamic = js("new BigInt64Array(16)")

        /**
         * An array of 16 64-bit unsigned integers.
         */
        internal val u64_arr: dynamic = js("new BigUint64Array(16)")

        // TODO: Find out what really need to be copied to make "is" working and only copy this!
        @Suppress("UNUSED_PARAMETER")
        internal fun copyPrototypeToPrototype(source: Any, target: Any) = js(
            """
            var sp = Object.getPrototypeOf(source);
            var tp = Object.getPrototypeOf(target);
            var symbols = Object.getOwnPropertySymbols(sp);
            var i;
            for (i in symbols) {
                var symbol = symbols[i];
                tp[symbol] = sp[symbol];
            };
            var desc = {enumerable:false,writable:true,value:null};
            var keys = Object.getOwnPropertyNames(sp);
            for (i in keys) {
                var key = keys[i];
                desc.value = sp[key];
                if ("constructor"==key) continue;
                Object.defineProperty(tp, key, desc);
            };"""
        )

        @JsStatic
        @Suppress("NON_EXPORTABLE_TYPE")
        actual fun initialize(): Boolean {
            if (!isInitialized) {
                isInitialized = true
                val objectTemplate = object : PlatformObject {}
                val listTemplate = object : PlatformList {
                    override fun <T : Proxy> proxy(klass: KClass<T>): T = proxy(this, klass)
                }
                val mapTemplate = object : PlatformMap {
                    override fun <T : Proxy> proxy(klass: KClass<T>): T = proxy(this, klass)
                }
                val dataViewTemplate = object : PlatformDataView {
                    override fun <T : Proxy> proxy(klass: KClass<T>): T = proxy(this, klass)
                }
                val symbolTemplate = object : Symbol {}
                val weakRefTemplate = object : WeakRef<Int> {
                    override fun deref(): Int = 0
                }

                // Fix that `if (longValue instanceof Number)` works.
//                js("""
//Object.defineProperty(Number, Symbol.hasInstance, {
//    value(x) {
//        return typeof x === "number" ||
//               typeof x === "bigint" ||
//               x instanceof Number;
//    }
//});
//""")
                copyPrototypeToPrototype(objectTemplate, js("{}").unsafeCast<Any>())
                copyPrototypeToPrototype(listTemplate, js("[]").unsafeCast<Any>())
                copyPrototypeToPrototype(mapTemplate, js("new Map()").unsafeCast<Any>())
                copyPrototypeToPrototype(dataViewTemplate, js("new DataView(new ArrayBuffer(0))").unsafeCast<Any>())

                copyPrototypeToPrototype(symbolTemplate, js("Symbol()").unsafeCast<Any>())
                copyPrototypeToPrototype(weakRefTemplate, js("new WeakRef(Object(0))").unsafeCast<Any>())
                return true
            }
            return false
        }

        @JsStatic
        actual val UNDEFINED: Any = js("undefined").unsafeCast<Any>()

        @JsStatic
        actual val DEFAULT_SYMBOL = symbol("com.here.naksha.lib.nak")

        @JsStatic
        actual val ITERATOR: Symbol = js("Symbol.iterator").unsafeCast<Symbol>()

        @JsStatic
        actual val INT64_MAX_VALUE: Long = Long.MAX_VALUE

        @JsStatic
        actual val INT64_MIN_VALUE: Long = Long.MIN_VALUE

        @JsStatic
        actual val MAX_SAFE_INT: Double = 9007199254740991.0

        @JsStatic
        actual val MIN_SAFE_INT: Double = -9007199254740991.0

        @JsStatic
        actual val EPSILON: Double = js("Number.EPSILON").unsafeCast<Double>()

        @JsStatic
        actual fun intern(s: String, cd: Boolean): String = js("(cd ? s.normalize('NFC') : s.normalize('NFKC'))").unsafeCast<String>()

        private fun symbol(key: String?): Symbol = js("(key ? Symbol.for(key) : Symbol())").unsafeCast<Symbol>()

        @JsStatic
        actual fun newMap(vararg entries: Any?): PlatformMap {
            val map = js("new Map()")
            if (entries.isNotEmpty()) {
                var i = 0
                while (i < entries.size) {
                    val key = entries[i++]
                    val value = if (i < entries.size) entries[i++] else null
                    map.set(key, value)
                }
            }
            return map.unsafeCast<PlatformMap>()
        }

        @JsStatic
        actual fun <K : Any, V : Any> newAtomicMap(): AtomicMap<K, V> = JsAtomicMap()

        @JsStatic
        actual fun <R: Any> newAtomicRef(startValue: R?): AtomicRef<R> = JsAtomicRef(startValue)

        @JsStatic
        actual fun <R: Any> newAtomicNonNullRef(startValue: R): AtomicNonNullRef<R> = JsAtomicNonNullRef(startValue)

        @JsStatic
        actual fun newAtomicBool(startValue: Boolean): AtomicBool = JsAtomicBool(startValue)

        @JsStatic
        actual fun newAtomicInt(startValue: Int): AtomicInt = JsAtomicInt(startValue)

        @JsStatic
        actual fun newAtomicInt64(startValue: Long): AtomicInt64 = JsAtomicInt64(startValue)

        @JsStatic
        actual fun newList(vararg entries: Any?): PlatformList {
            val array = js("[]").unsafeCast<PlatformList>()
            if (entries.isNotEmpty()) {
                var i = 0
                while (i < entries.size) {
                    js("array[i]=value")
                    i++
                }
            }
            return array
        }

        @JsStatic
        actual fun newByteArray(size: Int): ByteArray = ByteArray(size)

        @JsStatic
        actual fun newDataView(byteArray: ByteArray, offset: Int, size: Int): PlatformDataView =
            js("new DataView(byteArray.buffer, offset, size)").unsafeCast<PlatformDataView>()

        // Note: Some values in JavaScript must be wrapped into an object (like string literals or numbers), however,
        //       not all numbers of string literals, so we only wrap, when really necessary!

        @JsStatic
        actual fun <T : Any> newWeakRef(referent: T): WeakRef<T> =
            js("try { return new WeakRef(referent); } catch(e) { return new WeakRef(Object(referent)); }").unsafeCast<WeakRef<T>>()

        /**
         * Creates a new reentrant lock.
         * @return the created reentrant lock.
         */
        @JsStatic
        actual fun newLock(): PlatformLock = JsLock()

        @JsStatic
        actual fun unbox(value: Any?): Any? {
            if (value == null) return value
            if (value is Proxy) return value.platformObject()
            if (value is JsEnum) return value.value
            return if (isScalar(value)) value.asDynamic().valueOf() else value
        }

        @JsStatic
        actual fun toInt(value: Any): Int = when (value) {
            is Long -> value.toInt()
            is Number -> value.toInt()
            is String -> value.toInt()
            else -> throw IllegalArgumentException("Failed to convert object to int")
        }

        @JsStatic
        actual fun toInt64(value: Any): Long = when (value) {
            is Number -> value.toLong()
            is String -> value.toLong()
            else -> throw IllegalArgumentException("Failed to convert object to int64")
        }

        @JsStatic
        actual fun toDouble(value: Any): Double = when (value) {
            is Long -> value.toDouble()
            is Number -> value.toDouble()
            is String -> value.toDouble()
            else -> throw IllegalArgumentException("Failed to convert object to double")
        }

        @JsStatic
        actual fun intToInt64(value: Int): Long = value.toLong()

        @JsStatic
        @Suppress("NON_EXPORTABLE_TYPE")
        actual fun longToInt64(value: Long): Long = value

        internal fun longToLegacyInt64(value: Long): Int64 {
            val view = convertView
            view.setInt32(0, (value ushr 32).toInt())
            view.setInt32(4, value.toInt())
            return view.getBigInt64(0).unsafeCast<Int64>()
        }

        @Suppress("NON_EXPORTABLE_TYPE")
        internal fun int64ToLong(value: Int64): Long {
            val view = convertView
            view.setBigInt64(0, value)
            val hi = view.getInt32(0).unsafeCast<Int>()
            val lo = view.getInt32(4).unsafeCast<Int>()
            return ((hi.toLong() and 0xffff_ffff) shl 32) or (lo.toLong() and 0xffff_ffff).unsafeCast<Long>()
        }

        private val convertView: dynamic = js("new DataView(new ArrayBuffer(16))")

        @JsStatic
        actual fun toInt64RawBits(d: Double): Long = d.toRawBits()

        @JsStatic
        actual fun toDoubleRawBits(i: Long): Double = Double.fromBits(i)

        @JsStatic
        actual fun int64ToLong(value: Long): Long = value

        @JsStatic
        actual fun isNumber(o: Any?): Boolean =
            js("o && (typeof o.valueOf()==='number' || typeof o.valueOf()==='bigint')").unsafeCast<Boolean>()

        @JsStatic
        actual fun isScalar(o: Any?): Boolean {
            if (o == null) return true
            return when (jsTypeOf(o.asDynamic().valueOf())) {
                "string", "number", "bigint", "boolean" -> true
                else -> false
            }
        }

        @JsStatic
        actual fun isInteger(o: Any?): Boolean = js("o && (Number.isInteger(o) || typeof o.valueOf()==='bigint')").unsafeCast<Boolean>()

        @JsStatic
        actual fun isDouble(o: Any?): Boolean = o is Number

        @JsStatic
        actual fun compare(a: Any?, b: Any?): Int {
            TODO("Fix me, see documentation!")
        }

        @JsStatic
        actual fun hashCodeOf(o: Any?): Int {
            if (o == null) return 0
            val S = DEFAULT_SYMBOL
            val nak: dynamic = o
            if (nak[S] != null && jsTypeOf(nak[S].hashCode) == "function") {
                try {
                    return nak[DEFAULT_SYMBOL].hashCode().unsafeCast<Int>()
                } catch (ignore: Throwable) {
                }
            }
            // TODO: Fix me, see documentation!
            return Fnv1a32.string(Fnv1a32.start(), nak.toString())
        }

        private val assignables = HashMap<KClass<*>, HashMap<KClass<*>, Boolean>>()

        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual fun isAssignable(source: KClass<*>, target: KClass<*>): Boolean {
            var assignable = assignables[target]
            if (assignable == null) {
                assignable = HashMap()
                assignables[target] = assignable
            }
            var isAssignable = assignable[source]
            if (isAssignable == null) {
                isAssignable = target.isInstance(allocateInstance(source))
                assignable[source] = isAssignable
            }
            return isAssignable
        }

        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual fun isProxyKlass(klass: KClass<*>): Boolean = isAssignable(klass, Proxy::class)

        /**
         * Returns the [KClass] created **by** the given constructor. This is mainly for JavaScript, it will simply query a cached and if not
         * found, it will create an instance, query the [KClass] using [klassOf] and add it into the cache. Therefore, the cost of
         * creating an instance to get the [KClass] is only paid ones in the lifetime of an application.
         * @param constructor The constructor.
         * @return The [KClass] that is created **by** this constructor.
         * @throws IllegalArgumentException If the given constructor does not create any valid Kotlin object.
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        fun <T : Any> klassFor(constructor: KFunction<T>): KClass<out T>
            = (js("Object.create(constructor.prototype)") as T)::class

        @Suppress("NON_EXPORTABLE_TYPE", "UNCHECKED_CAST")
        @JsStatic
        actual fun <T : Any> klassForName(name: String): KClass<T> {
            @Suppress("CanBeVal") //
            var instance: T? = null
            js("""var i=0; var p=name.split("."); var k=globalThis;
while (k && i<p.length) k=k[p[i++]];
if (typeof k==='function') instance=Object.create(k.prototype);""")
            if (instance == null) throw IllegalArgumentException("Class not found '$name'")
            return instance::class as KClass<T>
        }

        @Suppress("NON_EXPORTABLE_TYPE", "UNCHECKED_CAST")
        @JsStatic
        actual fun <T : Any> klassOf(o: T): KClass<T> = o::class as KClass<T>

        /**
         * The KClass for [Any].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val anyKlass: KClass<Any> = Any::class

        /**
         * The KClass for [Boolean].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val booleanKlass: KClass<Boolean> = Boolean::class

        /**
         * The KClass for [Short].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val shortKlass: KClass<Short> = Short::class

        /**
         * The KClass for [Int].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val intKlass: KClass<Int> = Int::class

        /**
         * The KClass for [Long].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val int64Klass: KClass<Long> = Long::class

        /**
         * The KClass for [Double].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val doubleKlass: KClass<Double> = Double::class

        /**
         * The KClass for [String].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val stringKlass: KClass<String> = String::class

        /**
         * The KClass for [PlatformObject].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val objectKlass: KClass<PlatformObject> = PlatformObject::class

        /**
         * The KClass for [PlatformList].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val listKlass: KClass<PlatformList> = PlatformList::class

        /**
         * The KClass for [PlatformMap].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val mapKlass: KClass<PlatformMap> = PlatformMap::class

        /**
         * The KClass for [PlatformDataViewApi].
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual val dataViewKlass: KClass<PlatformDataView> = PlatformDataView::class

        /**
         * Tests if the given value is _null_ or _undefined_.
         * @param any The value to test.
         * @return _true_ if the value is _null_ or _undefined_; false otherwise.
         */
        @JsStatic
        actual fun isNil(any: Any?): Boolean = js("any===null || any===undefined").unsafeCast<Boolean>()

        /**
         * Creates a new instance of the given type.
         * @param klass The type of which to create a new instance.
         * @return The new instance.
         * @throws IllegalArgumentException If the given class does not have a parameterless constructor.
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual fun <T : Any> newInstanceOf(klass: KClass<out T>): T {
            try {
                return klass.createInstance()
            } catch (e: Exception) {
                if (e is IllegalArgumentException) throw e
                throw IllegalArgumentException(e)
            }
        }

        /**
         * Creates a new instance of the given type, bypassing the constructor, so it returns the uninitialized class.
         * @param klass The type of which to create a new instance.
         * @return The new instance.
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual fun <T : Any> allocateInstance(klass: KClass<out T>): T {
            // We can bypass the constructor, but before we do this, we need to ensure that the companion object
            // is created, and all other things of the class are ready. We can only do this by initializing the class!
            initializeKlass(klass)
            val constructor = klass.js
            return js("Object.create(constructor.prototype)").unsafeCast<T>()
        }

        private val initializedClasses = HashMap<KClass<*>, Boolean>()

        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual fun initializeKlass(klass: KClass<*>) {
            if (!initializedClasses.containsKey(klass)) {
                try {
                    klass.createInstance()
                } catch (ignore: Throwable) {
                } finally {
                    initializedClasses[klass] = true
                }
            }
        }

        @JsStatic
        actual fun <T> copy(obj: T?, recursive: Boolean): T? = js("""
if (typeof obj !== 'object' || obj === null) return obj;
if (Array.isArray(obj)) {
  if (recursive) {
    var copy = [];
    for (v in obj) copy.push(arguments.callee(item, true));
    return copy;
  }
  return obj.slice();
}
if (obj instanceof Map) {
  if (recursive) {
    var map = new Map();
    var it = obj[Symbol.iterator]();
    var e = it.next();
    while (!e.done) {
      map.set(e.value[0], arguments.callee(e.value[1], true));
      e = it.next();
    }
    return map;
  }
  return new Map(obj);
}
if (obj instanceof DataView) {
  return new DataView(obj.buffer.slice(0));
}
if (obj instanceof Object) {
  if (recursive) {
    var map = new Map();
    for (key in obj) {
        if (obj.hasOwnProperty(key)) map.set(key, arguments.callee(obj[key], true));
    }
    return map;
  }
  return new Map(Object.entries(v));
}
return obj;
""").unsafeCast<T?>()

        @JsName("toJson")
        @JsStatic
        actual fun toJSON(obj: Any?): String = toJSON(obj, ToJsonOptions.DEFAULT)

        @JsName("toJsonWithOptions")
        @JsStatic
        actual fun toJSON(obj: Any?, options: ToJsonOptions): String {
            val o = if (obj is Proxy) obj.platformObject() else obj
            return js(
                """JSON.stringify(o, function(k, v) {
  if (!v) return v;
  if (v.valueOf() instanceof Map) return Object.fromEntries(v.valueOf().entries());
  if (typeof v.valueOf() === "bigint") return "data:bigint;dec,"+String(v);
  return v;
})"""
            ).unsafeCast<String>()
        }

        @JsName("fromJson")
        @JsStatic
        actual fun fromJSON(json: String): Any? = fromJSON(json, FromJsonOptions.DEFAULT)

        @JsName("fromJsonWithOptions")
        @JsStatic
        actual fun fromJSON(json: String, options: FromJsonOptions): Any? = js(
            """JSON.parse(json, function(k, v) {
  if (!v) return v;
  if (typeof v === "string" && v.startsWith("data:bigint")) {
    var i = v.indexOf(",");
    return BigInt(v.substring(i+1));
  }
  if (!Array.isArray(v) && typeof v === "object") return new Map(Object.entries(v));
  return v;
})"""
        ).unsafeCast<Any?>()

        /**
         * Convert the given platform native objects recursively into multi-platform objects. So all maps are corrected to [PlatformMap],
         * all strings starting with `data:bigint,` or Java `Long`'s are converted into [Int64]'s, lists are corrected to [PlatformList],
         * and so on. This can be used after a JSON was parsed from an arbitrary platform tool into some platform specific standard
         * objects or when exchanging data with a platform specific library that does not like the multi-platform objects.
         * @param obj The platform native objects to convert recursively.
         * @param importers The importers to use.
         * @return The given platform native objects converted into multi-platform objects.
         */
        @JsStatic
        actual fun fromPlatform(obj: Any?, importers: List<PlatformImporter>): Any? {
            TODO("Not yet implemented fromPlatform")
        }

        /**
         * Convert the given multi-platform objects recursively into the default platform native objects, for example [PlatformMap] may
         * become a pure `Object` in JavaScript. This is often useful when exchanging code with libraries that do not support `Map`.
         * In Java this will convert to [PlatformMap] to [LinkedHashMap].
         * @param obj The multi-platform objects to be converted into platform native objects.
         * @param exporters The exporters to use.
         * @return The platform native objects.
         */
        @JsStatic
        actual fun toPlatform(obj: Any?, exporters: List<PlatformExporter>): Any? {
            TODO("Not yet implemented toPlatform")
        }

        /**
         * Create a proxy or return the existing proxy. If a proxy of a not compatible type exists already and [doNotOverride]
         * is _true_, the method will throw an _IllegalStateException_; otherwise the current type is simply overridden.
         * @param pobject The object at which to query for the proxy.
         * @param klass The proxy class.
         * @param doNotOverride If _true_, do not override existing symbols bound to incompatible types, but throw an [IllegalStateException]
         * @return The proxy instance.
         * @throws IllegalStateException If [doNotOverride] is _true_ and the symbol is already bound to an incompatible type.
         */
        @Suppress("NON_EXPORTABLE_TYPE")
        @JsStatic
        actual fun <T : Proxy> proxy(pobject: PlatformObject, klass: KClass<T>, doNotOverride: Boolean): T {
            val o = pobject.asDynamic()
            val sym = Symbols.of(klass)
            val p = o[sym]
            if (klass.isInstance(p)) return p.unsafeCast<T>()
            check(!doNotOverride || p !is Proxy) { "new proxy forbidden, because doNotOverride set" }
            val proxy = klass.createInstance()
            proxy.bind(pobject, sym)
            return proxy
        }

        /**
         * The [PlatformLogger].
         */
        @JsStatic
        actual val logger: PlatformLogger = JsLogger()

        /**
         * Creates a new thread-local. Should be stored only in a static immutable variable (`val`).
         * @param initializer An optional lambda to be invoked, when the thread-local is read for the first time.
         * @return The thread local.
         */
        @JsStatic
        actual fun <T> newThreadLocal(initializer: (() -> T)?): PlatformThreadLocal<T> = JsThreadLocal(initializer)

        // TODO: Implement high resolution timer, when available (sadly, not in PLV8):
        //       https://developer.mozilla.org/en-US/docs/Web/API/Performance/now

        /**
         * Returns the current epoch milliseconds.
         * @return The current epoch milliseconds.
         */
        @JsStatic
        actual fun currentMillis(): Long = js("Date.now()").unsafeCast<Double>().toLong()

        /**
         * Returns the current epoch microseconds.
         * @return current epoch microseconds.
         */
        @JsStatic
        actual fun currentMicros(): Long = js("Date.now()*1000").unsafeCast<Double>().toLong()

        /**
         * Returns the current epoch nanoseconds.
         * @return current epoch nanoseconds.
         */
        @JsStatic
        actual fun currentNanos(): Long = js("Date.now()*1000*1000").unsafeCast<Double>().toLong()

        /**
         * Generates a new random number between 0 and 1 (therefore with 53-bit random bits).
         * @return The new random number between 0 and 1.
         */
        @JsStatic
        actual fun random(): Double = js("Math.random()").unsafeCast<Double>()

        private const val MANTISSA_MASK = 0x000f_ffff_ffff_ffffL
        private const val MANTISSA_LO_MASK = 0x0000_0000_1fff_ffffL

        /**
         * Tests if the given 64-bit floating point number can be converted into a 32-bit floating point number without losing information.
         * @param value The 64-bit floating point number.
         * @return _true_ if the given 64-bit float can be converted into a 32-bit one without losing information; _false_ otherwise.
         */
        @JsStatic
        actual fun canBeFloat32(value: Double): Boolean {
            // IEEE-754, 32-bit = One sign-bit, 8-bit exponent biased by 127, then 23-bit mantissa
            // IEEE-754, 64-bit = One sign-bit, 11-bit exponent biased by 1023, then 52-bit mantissa
            // E = 0 means denormalized number (M>0) or null (M=0)
            // E = 255|2047 means either endless (M=0) or not a number (M>0)
            val view = convertView
            view.setFloat64(0, value)
            var exponent = (view.getInt16(0).unsafeCast<Int>() ushr 4) and 0x7ff
            if (exponent == 0 || exponent == 2047) return false
            // Remove bias: -1023 (0) .. 1024 (2047)
            exponent -= 1023
            // 32-bit exponent is 8-bit with bias 127: -127 (0) .. 128 (255)
            // We want to avoid extremes as they encode special states.
            if (exponent < -126 || exponent > 127) return false
            // We do not want to lose precision in mantissa either.
            // Either the lower 29-bit of mantissa are zero (only 23-bit used) or all bits are set.
            val mantissa = value.toRawBits() and MANTISSA_MASK
            return (mantissa and MANTISSA_LO_MASK) == 0L || mantissa == MANTISSA_MASK
        }

        private const val MIN_INT_VALUE_AS_DOUBLE = Int.MIN_VALUE.toDouble()
        private const val MAX_INT_VALUE_AS_DOUBLE = Int.MAX_VALUE.toDouble()

        /**
         * Tests if the given 64-bit floating point number can be converted into a 32-bit integer without losing information.
         * @param value The 64-bit floating point number.
         * @return _true_ if the given 64-bit float can be converted into a 32-bit integer without losing information; _false_ otherwise.
         */
        @JsStatic
        actual fun canBeInt32(value: Double): Boolean {
            val rounded = round(value)
            return rounded == value && (rounded in MIN_INT_VALUE_AS_DOUBLE..MAX_INT_VALUE_AS_DOUBLE)
        }

        /**
         * Tests if this code is executed within a PostgresQL database using [PLV8 extension](https://plv8.github.io/).
         * @return _true_ if this code is executed within PostgresQL database using [PLV8 extension](https://plv8.github.io/).
         */
        @JsStatic
        actual fun isPlv8(): Boolean = js("typeof plv8==='object'").unsafeCast<Boolean>()

        @JsStatic
        actual fun encodeURIComponent(uriComponent: String): String = js("encodeURIComponent(uriComponent)") as String

        @JsStatic
        actual fun decodeURIComponent(encodedURI: String): String = js("decodeURIComponent(encodedURI)") as String

        /**
         * Calculates the MD5 hash about the given text.
         *
         * @param text the text to hash.
         * @return the MD5 hash, being a byte-array with size 16 (128-bit).
         */
        @JsStatic
        actual fun md5(text: String): ByteArray {
            if (isPlv8()) return js("plv8.execute(\"SELECT digest(\$1,'md5') as i\",[text])[0].i").unsafeCast<ByteArray>()
            // TODO: Use SubtleCrypto-API in the browser: https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto
            throw UnsupportedOperationException("md5 is not implemented in the current environment")
        }

        /**
         * Calculates the MD5 hash about the given text.
         *
         * @param text the text to hash.
         * @return the MD5 hash, being a byte-array with size 16 (128-bit).
         */
        @JsName("md5Bytes")
        @JsStatic
        actual fun md5(bytes: ByteArray): ByteArray {
            if (isPlv8()) return js("plv8.execute(\"SELECT digest(\$1,'md5') as i\",[bytes])[0].i").unsafeCast<ByteArray>()
            // TODO: Use SubtleCrypto-API in the browser: https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto
            throw UnsupportedOperationException("md5 is not implemented in the current environment")
        }

        /**
         * Compress bytes.
         * @param raw the bytes to compress.
         * @return the deflated (compressed) bytes.
         */
        @JsStatic
        actual fun lz4Deflate(raw: ByteArray): ByteArray {
            if (isPlv8()) js("""plv8.execute('SELECT lz4_deflate($1::bytea)::bytea as c',[raw])[0].c""").unsafeCast<ByteArray>()
            // TODO: Use Stream-API in the browser: https://developer.mozilla.org/en-US/docs/Web/API/Streams_API
            throw UnsupportedOperationException("lz4Deflate is not implemented in the current environment")
        }

        /**
         * Decompress bytes.
         * @param compressed the bytes to decompress.
         * @return the inflated (raw) bytes.
         */
        @JsStatic
        actual fun lz4Inflate(compressed: ByteArray): ByteArray {
            if (isPlv8()) js("""plv8.execute('SELECT lz4_inflate($1::bytea)::bytea as c',[compressed])[0].c""").unsafeCast<ByteArray>()
            // TODO: Use Stream-API in the browser: https://developer.mozilla.org/en-US/docs/Web/API/Streams_API
            throw UnsupportedOperationException("lz4Inflate is not implemented in the current environment")
        }

        /**
         * Compress bytes.
         * @param raw the bytes to compress.
         * @return the deflated (compressed) bytes.
         */
        @JsStatic
        actual fun gzipDeflate(raw: ByteArray): ByteArray {
            if (isPlv8()) js("""plv8.execute('SELECT gzip($1::bytea)::bytea as c',[raw])[0].c""").unsafeCast<ByteArray>()
            // TODO: Use Stream-API in the browser: https://developer.mozilla.org/en-US/docs/Web/API/Streams_API
            throw UnsupportedOperationException("lz4Inflate is not implemented in the current environment")
        }

        /**
         * Decompress bytes.
         * @param compressed The bytes to decompress.
         * @return the inflated (raw) bytes.
         */
        @JsStatic
        actual fun gzipInflate(compressed: ByteArray): ByteArray {
            if (isPlv8()) js("""plv8.execute('SELECT gunzip($1::bytea)::bytea as c',[compressed])[0].c""").unsafeCast<ByteArray>()
            // TODO: Use Stream-API in the browser: https://developer.mozilla.org/en-US/docs/Web/API/Streams_API
            throw UnsupportedOperationException("lz4Inflate is not implemented in the current environment")
        }

        @JsStatic
        actual fun stackTrace(t: Throwable): String = t.stackTraceToString()

        @JsStatic
        actual fun normalize(value: String, form: NormalizerForm): String {
            val formString = form.name
            return js("value.normalize(formString)").unsafeCast<String>()
        }

        init {
            initialize()
        }

        @JsStatic
        actual fun fromJSON(utf8: ByteArray): Any? {
            TODO("Not yet implemented")
        }

        @JsStatic
        actual fun getTestStorageId(): String = "local_psql_test_storage"
        actual val FAL: String
            get() {
                if (!isPlv8()) return ""
                return js("""
(function() {
    var orig = Error.prepareStackTrace;
    Error.prepareStackTrace = function(_, stack) { return stack; };
    var err = new Error();
    Error.captureStackTrace(err, callerInfo);
    var frame = err.stack[0];
    Error.prepareStackTrace = orig;
    return "[" + frame.getFileName() + ":" + frame.getLineNumber() + "] ";
})()
""").unsafeCast<String>()
            }

        actual fun fal(n: Int): String {
            if (!isPlv8()) return ""
            return js("""
(function() {
    var orig = Error.prepareStackTrace;
    Error.prepareStackTrace = function(_, stack) { return stack; };
    var err = new Error();
    Error.captureStackTrace(err, callerInfo);
    var frame = err.stack[Math.max(0, (n||1)-1) || 0];
    Error.prepareStackTrace = orig;
    return "[" + frame.getFileName() + ":" + frame.getLineNumber() + "] ";
})()
""").unsafeCast<String>()
        }
    }
}
