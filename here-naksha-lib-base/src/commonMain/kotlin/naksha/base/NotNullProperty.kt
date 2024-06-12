package naksha.base

import kotlin.js.JsExport
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * The description of a property stored in a map.
 *
 * It is recommended to add this delegator statically to avoid that for every object instance a new instance
 * of the delegator is created. The Kotlin compiler will even inline the getter/setter calls in that case:
 * ```kotlin
 * class Foo : P_Object() {
 *   companion object {
 *     private val NAME = NullableProperty<Any, Foo, String>
 *                        (String::class)
 *     private val AGE = NotNullProperty<Any, Foo, Int>
 *                        (Int::class, 0)
 *   }
 *   var name: String? by NAME
 *   var age: Int by AGE
 * }
 * ```
 * @param MAP_VALUE_TYPE The type of the map value.
 * @param MAP The type of the map to which to attach the property.
 * @param PROPERTY_TYPE The type of the property, must have [MAP_VALUE_TYPE] as super type.
 * @property klass The type of the property.
 * @property defaultValue The default value to return, when reading the member while being _null_.
 * @property name The name of the property in the underlying map, if different from the property name.
 */
@Suppress("NON_EXPORTABLE_TYPE", "OPT_IN_USAGE")
@JsExport
open class NotNullProperty<MAP_VALUE_TYPE : Any, MAP : P_Map<String, MAP_VALUE_TYPE>, PROPERTY_TYPE : MAP_VALUE_TYPE>(
    val klass: KClass<out PROPERTY_TYPE>,
    val defaultValue: PROPERTY_TYPE? = null,
    val name: String? = null
) {
    open operator fun getValue(self: MAP, property: KProperty<*>): PROPERTY_TYPE {
        val key = this.name ?: property.name
        if (defaultValue == null) return self.getOrCreate(key, klass)
        return self.getOrSet(key, defaultValue)
    }

    open operator fun setValue(self: MAP, property: KProperty<*>, value: PROPERTY_TYPE) = self.put(this.name ?: property.name, value)
}