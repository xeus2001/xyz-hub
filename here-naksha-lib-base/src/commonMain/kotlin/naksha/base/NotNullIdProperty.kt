package naksha.base

import naksha.base.Platform.PlatformCompanion.fal
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KProperty

/**
 * The description of an `id` property stored in a map.
 *
 * It is recommended to add this delegator statically to avoid that for every object instance a new instance of the delegator is created.
 * The Kotlin compiler will even inline the getter/setter calls in that case:
 * ```kotlin
 * class Foo: AnyObject() {
 *   companion object {
 *     private val ID = IdProperty<Foo>()
 *   }
 *   var id: Id by ID
 * }
 * ```
 * @param MAP the type of the map to which to attach the property.
 * @property randomId if the value of the property is `null` and the initializer does return `null`, if a random identifier should be generated; otherwise a [NakshaException] with [ILLEGAL_STATE][NakshaError.ILLEGAL_STATE] is thrown. If `null` is assigned and `randomId` is _false_, then an [ILLEGAL_ARGUMENT][NakshaError.ILLEGAL_ARGUMENT] will be raised; defaults to _true_.
 * @property name the name of the property in the map, if different from the property name, if `null`, the property name is used.
 * @property init the initializer to create a new identifier, when the property does not exist or the value is not of the desired type.
 * @see NullableIdProperty
 */
@Suppress("NON_EXPORTABLE_TYPE", "OPT_IN_USAGE")
@JsExport
open class NotNullIdProperty<MAP : MapProxy<String, Any>>(
    val randomId: Boolean = true,
    val name: String? = null,
    val init: ((self: MAP, name: String) -> Id)? = null,
) {
    private fun name(propertyName: String?): String
        = this.name ?: propertyName ?: throw illegalArg("Undefined property name")

    @JsName("getValueByProperty")
    open operator fun getValue(self: MAP, property: KProperty<*>): Id = get_value(self, property.name)

    @JvmOverloads
    open fun getValue(self: MAP, propertyName: String? = null): Id = get_value(self, propertyName)

    private fun get_value(self: MAP, propertyName: String? = null): Id {
        val name = name(propertyName)
        var id = self.getId( name, true)
        if (id == null) {
            id = init?.invoke(self, name)
            if (id == null) {
                if (!randomId) throw illegalState("${fal(3)}'$name' is null or no valid Id instance")
                id = Id()
            }
            self.setRaw(name, id)
        }
        return id
    }

    @JsName("setValueByProperty")
    open operator fun setValue(self: MAP, property: KProperty<*>, value: Id)
        = setValue(self, name(property.name), value)

    @JvmOverloads
    open fun setValue(self: MAP, propertyName: String? = null, value: Id)
        = self.put(name(propertyName), value)
}

