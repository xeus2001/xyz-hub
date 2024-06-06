@file:Suppress("OPT_IN_USAGE")

package com.here.naksha.lib.base

import com.here.naksha.lib.base.Platform.Companion.isNil
import com.here.naksha.lib.base.PlatformMapApi.Companion.map_clear
import com.here.naksha.lib.base.PlatformMapApi.Companion.map_contains_key
import com.here.naksha.lib.base.PlatformMapApi.Companion.map_contains_value
import com.here.naksha.lib.base.PlatformMapApi.Companion.map_get
import com.here.naksha.lib.base.PlatformMapApi.Companion.map_remove
import com.here.naksha.lib.base.PlatformMapApi.Companion.map_set
import com.here.naksha.lib.base.PlatformMapApi.Companion.map_size
import kotlin.js.JsExport
import kotlin.js.JsStatic
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

/**
 * A map that is not thread-safe.
 */
@Suppress("NON_EXPORTABLE_TYPE", "UNCHECKED_CAST")
@JsExport
abstract class P_Map<K:Any, V:Any>(val keyKlass: KClass<out K>, val valueKlass: KClass<out V>) : Proxy(), MutableMap<K, V?> {
    /**
     * Helper to return the value of the key, if the key does not exist or is not of the expected type, the
     * provided alternative is returned and the key is set to the alternative.
     * @param <T> The expected type.
     * @param key The key to query.
     * @param alternative The alternative to set and return, when the key does not exist or the value is not of the expected type.
     * @return The value.
     */
    protected fun <T : Any> getOrSet(key: K, alternative: T): T {
        val data = data()
        val raw = map_get(data, key)
        var value = box(raw, Platform.klassOf(alternative))
        if (value == null) {
            value = alternative
            map_set(data, key, unbox(value))
        }
        return value
    }

    /**
     * Helper to return the value of the key, if the key does not exist or is not of the expected type, a new
     * value is created, stored with the key and returned.
     * @param <T> The expected type.
     * @param key The key to query.
     * @param klass The [KClass] of the expected value.
     * @return The value.
     */
    protected fun <T : Any> getOrCreate(key: K, klass: KClass<out T>): T {
        val data = data()
        val raw = map_get(data, key)
        var value = box(raw, klass)
        if (value == null) {
            value = Platform.newInstanceOf(klass)
            map_set(data, key, unbox(value))
        }
        return value
    }

    /**
     * Helper to return the value of the key, if the key does not exist or is not of the expected type, the
     * provided alternative is returned.
     * @param <T> The expected type.
     * @param key The key to query.
     * @param alternative The alternative to return, when the key does not exist or the value is not of the expected type.
     * @return The value.
     */
    protected fun <T : Any> getAs(key: K, klass: KClass<out T>, alternative: T): T {
        val data = data()
        val raw = map_get(data, key)
        val value = box(raw, klass)
        return value ?: alternative
    }

    /**
     * Helper to return the value of the key, if the key does not exist or is not of the expected type, _null_ is returned.
     * @param <T> The expected type.
     * @param key The key to query.
     * @return The value or _null_.
     */
    protected fun <T : Any> getOrNull(key: K, klass: KClass<out T>): T? = box(map_get(data(), key), klass)

    /**
     * Convert the given value into a key.
     * @param value The value to convert.
     * @param alt The alternative to return when the value can't be cast.
     * @return The given value as key.
     */
    protected open fun toKey(value: Any?, alt: K? = null): K? = box(value, keyKlass, alt)

    /**
     * Convert the given value into a value.
     * @param key The key for which to convert the value.
     * @param value The value to convert.
     * @param alt The alternative to return when the value can't be cast.
     * @return The given value as value.
     */
    protected open fun toValue(key: K, value: Any?, alt: V? = null): V? = box(value, valueKlass, alt)

    override fun createData(): PlatformMap = Platform.newMap()
    override fun data(): PlatformMap = super.data() as PlatformMap

    override val entries: MutableSet<MutableMap.MutableEntry<K, V?>>
        get() = TODO("Not yet implemented")
    override val keys: MutableSet<K>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = map_size(data())
    override val values: MutableCollection<V?>
        get() = TODO("Not yet implemented")

    override fun clear() = map_clear(data())

    override fun isEmpty(): Boolean = map_size(data()) == 0

    override fun remove(key: K): V? = toValue(key, map_remove(data(), key))

    override fun putAll(from: Map<out K, V?>) {
        TODO("Not yet implemented")
    }

    fun addAll(vararg items: Any?) {
        val data = data()
        var i = 0
        while (i < items.size) {
            val key = toKey(items[i++])
            val value = if (i < items.size) unbox(items[i++]) else null
            require(key != null)
            map_set(data, key, value)
        }
    }

    override fun put(key: K, value: V?): V? = toValue(key, map_set(data(), key, unbox(value)))

    override fun get(key: K): V? = toValue(key, map_get(data(), key))

    override fun containsValue(value: V?): Boolean = map_contains_value(data(), value)

    override fun containsKey(key: K): Boolean = map_contains_key(data(), key)
}