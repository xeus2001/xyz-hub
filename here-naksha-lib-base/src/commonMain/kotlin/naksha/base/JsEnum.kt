@file:Suppress("LeakingThis")

package naksha.base

import naksha.base.fn.Fx1
import kotlin.js.JsExport
import kotlin.js.JsStatic
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

/**
 * A custom enumeration implementation that supports more flexible enumerations. Creating enumeration values
 * requires to implement [initClass], [init], and [namespace]. The implementation is only done in the class
 * directly extending the [JsEnum], which is called as well the **namespace**.
 *
 * The constructor should be kept _protected_ or _private_, because the creation of values through any other
 * means than [get] or as static member should be avoided. The default implementation looks like:
 * ```
 * open class Vehicle protected constructor(value: String)
 *   : JsEnum(value)
 * {
 *   override fun initClass() {
 *     register(Vehicle::class, Vehicle::class)
 *     register(Car::class, Vehicle::class)
 *     register(Truck::class, Vehicle::class)
 *   }
 *   override fun namespace(): KClass<out JsEnum> = Vehicle::class
 *   override fun init() {}
 *   open fun type(): String = "Vehicle"
 * }
 * class Car private constructor(value: String) : Vehicle(value) {
 *   companion object {
 *     @JvmStatic
 *     @JsStatic
 *     val BAR = Car("bar")
 *   }
 *   override fun type(): String = "Car"
 * }
 * class Truck private constructor(value: String) : Vehicle(value) {
 *   companion object {
 *     @JvmStatic
 *     @JsStatic
 *     val FOO = Truck("foo")
 *   }
 *   override fun type(): String = "Truck"
 * }
 * ```
 * Ones created like in the above example, the constants can be used like:
 * ```
 * val bar = JsEnum.get("bar", Vehicle::class)
 * val foo = JsEnum.get("foo", Vehicle::class)
 * val unknown = JsEnum.get("unknown", Vehicle::class)
 * println("$bar is ${bar.type()}")
 * println("$foo is ${foo.type()}")
 * println("$unknown is ${unknown.type()}")
 * ```
 * This should print "bar is Car", "foo is Truck" and "unknown is Vehicle".
 *
 * @constructor Create a new pre-defined enumeration value.
 * @param value The value for which to create a pre-defined value.
 * @property namespace The root Kotlin class that forms the namespace for all value, so the Kotlin class that directly extends [JsEnum].
 */
@Suppress("OPT_IN_USAGE", "NON_EXPORTABLE_TYPE")
@JsExport
abstract class JsEnum protected constructor(value: Any?) : CharSequence {
    /**
     * The value that represents NULL in the internal registry.
     */
    private object NULL

    /**
     * The value, either [String], [Int], [Int64], [Double], [Boolean] or _null_.
     */
    var value: Any? = null
        private set

    /**
     * Cached string representation to [toString]. When first called, [createString] is invoked to generate it.
     */
    private var string: String? = null

    /**
     * If the enumeration value is predefined or was generated on-the-fly. This can be used to reject custom
     * enumeration values.
     */
    var isDefined: Boolean = false
        private set

    init {
        var registerNamespace = false
        var ns = klassToNamespace[this::class]
        if (ns == null) {
            // This is the first time an instance of this enumeration type is created!
            ns = namespace()
            registerNamespace = true
        }
        val raw = alignValue(value)
        this.value = raw
        this.isDefined = true
        val nsMap = nsMap(ns)
        check(nsMap.putIfAbsent(raw ?: NULL, this) == null)
        if (registerNamespace) initClass()
    }

    companion object {
        private fun alignValue(value: Any?): Any? {
            if (value == null) return null
            // Note: Byte, Short, Integer will be converted to Long
            //       Float is converted to Double.
            // This simplifies usage and avoids that a number parsed into a short is not found when being pre-defined.
            return when (value) {
                is Byte -> value.toInt()
                is Short -> value.toInt()
                is Int -> value
                is Long -> Int64(value)
                is Int64 -> value
                is String -> value
                is Float -> value.toDouble()
                is Double -> value
                // Unknown number types are simply converted to double
                is Number -> value.toDouble()
                // Anything else becomes a string
                else -> value.toString()
            }
        }

        /**
         * A mapping between the class and the namespace. The namespace is the "root" class, so the class that directly
         * extends [JsEnum].
         */
        private val klassToNamespace = CMap<KClass<out JsEnum>, KClass<out JsEnum>>()

        /**
         * All registered enumeration values of a namespace. The first level is the namespace (the Kotlin class
         * that directly extend [JsEnum]), the second level maps values to registered instances.
         */
        private val registry = CMap<KClass<*>, CMap<Any, JsEnum>>()

        private fun nsMap(ns: KClass<out JsEnum>): CMap<Any, JsEnum> {
            var nsMap = registry[ns]
            if (nsMap == null) {
                nsMap = CMap()
                val existing = registry.putIfAbsent(ns, nsMap)
                if (existing != null) nsMap = existing
            }
            return nsMap
        }

        /**
         * Returns the enumeration instance for the given value and namespace. If the value is pre-defined, the
         * singleton is returned, otherwise a new instance is created.
         * @param value The value for which to return the enumeration.
         * @param enumKlass The enumeration klass to query.
         * @return The enumeration for the given value.
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        @JsStatic
        fun <T : JsEnum> get(value: Any?, enumKlass: KClass<out T>): T {
            var ns = klassToNamespace[enumKlass]
            if (ns == null) {
                // The class is not yet initialized, allocate an instance.
                // Allocating an instance, should cause the companion object to be initialized.
                val instance = Platform.allocateInstance(enumKlass)
                ns = klassToNamespace[enumKlass]
                if (ns == null) {
                    // Edge case: The enumeration class exists, but has no pre-defined values!
                    // Calling registerAll should at least register the namespace.
                    instance.initClass()
                    ns = klassToNamespace[instance.namespace()]
                }
                require(ns != null)
            }
            val nsMap = nsMap(ns)
            val key = alignValue(value) ?: NULL
            var e = nsMap[key] as T?
            if (e == null) {
                // The value is not pre-defined, create it on-the-fly.
                e = Platform.allocateInstance(enumKlass)
                e.value = alignValue(value)
                e.isDefined = false
                e.init()
            }
            return e
        }
    }

    /**
     * Returns the namespace of this instance. The namespace is the root enumeration type, so the type that
     * directly extends [JsEnum]. Should simply be implemented as:
     * ```
     * val namespace: KClass<out JsEnum> = this::class
     * ```
     * @return The namespace, the Kotlin class that directly extends [JsEnum].
     */
    abstract fun namespace(): KClass<out JsEnum>

    /**
     * This method is invoked exactly ones per namespace, when the enumeration namespace is not yet initialized. It
     * simplifies auto-initialization. Actually, it is required that the namespace class (the class directly extending
     * the [JsEnum]) implements this method and invokes [register] for itself and all extending classes. For example,
     * when an enumeration class `Vehicle` is created with two extending enumeration classes being `Car` and `Truck`,
     * then the `initClass` method of the `Vehicle` should do:
     * ```
     * protected fun initClass() {
     *   register(Vehicle::class, Vehicle::class)
     *   register(Car::class, Vehicle::class)
     *   register(Truck::class, Vehicle::class)
     * }
     * ```
     * This is needed to resolve the chicken-egg problem of the JVM class loading mechanism. The order is not relevant.
     *
     * **Note**: The minimal requirement is to register itself, like
     * ```
     * register(Self::class, Self::class)
     * ```
     *
     * ## Details
     *
     * There is a chicken-egg problem in the JVM. A class is not loaded before it is needed and even when the class is
     * loaded, it is not directly initialized. In other words, when we create an enumeration class and make constants for
     * possible value, the JVM runtime will not be aware of them unless we force it to load and initialize the class.
     * This means, unless one of the constants are really used, Jackson or other deserialization tools will not be able
     * to deserialize the value. This can lead to serious errors. This initialization method prevents this kind of error.
     */
    protected abstract fun initClass()

    /**
     * This method is invoked if the enumeration value is created via reflection by [get], it should initialize properties
     * to default values, because the constructor is bypassed by the reflection construction. It is invoked after the [value]
     * has been set, so [value] can be read.
     */
    protected abstract fun init()

    /**
     * A method that can be overridden, if the enumeration requires special handling in turning the value into a string.
     */
    protected open fun createString(): String = value?.toString() ?: "null"

    /**
     * Register a enumeration type.
     * @param childKlass The enumeration-class.
     * @param namespace The namespace.
     */
    protected fun <NS : JsEnum, C : NS> register(childKlass: KClass<out C>, namespace: KClass<out NS>) {
        val existing = klassToNamespace.putIfAbsent(childKlass, namespace)
        check(existing == null || existing === namespace)
        Platform.initializeKlass(childKlass)
    }

    /**
     * Runs a lambda against this enumeration instance. In Kotlin it is better to simply use `.apply {}`.
     *
     * @param selfKlass reference to the class of this enumeration-type.
     * @param lambda the lambda to call with the first parameter being this.
     * @return this.
     */
    @Suppress("UNCHECKED_CAST")
    fun <SELF : JsEnum> with(selfKlass: KClass<SELF>, lambda: Fx1<SELF>): SELF {
        lambda.call(this as SELF)
        return this
    }

    /**
     * Can be used with defined values to add aliases.
     *
     * @param selfClass Reference to the class of this enumeration-type.
     * @param value     The additional value to register.
     * @param <SELF>    The type of this.
     * @return this.
     */
    protected fun <SELF : JsEnum> alias(selfClass: KClass<SELF>, value: Any?) {

    }

    final override fun toString(): String {
        var s = this.string
        if (s == null) {
            s = createString()
            this.string = s
        }
        return s
    }

    final override fun equals(other: Any?): Boolean = this === other || (other is JsEnum && other.value == value)
    final override fun hashCode(): Int = toString().hashCode()

    final override val length: Int
        get() = toString().length

    final override fun get(index: Int): Char = toString()[index]

    final override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = toString().subSequence(startIndex, endIndex)
}