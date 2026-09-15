@file:Suppress("OPT_IN_USAGE")

package naksha.base

import naksha.base.PlatformListApi.PlatformListApiCompanion.array_get
import naksha.base.PlatformListApi.PlatformListApiCompanion.array_get_length
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_clear
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_contains_key
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_contains_value
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_get
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_iterator
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_remove
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_set
import naksha.base.PlatformMapApi.PlatformMapApiCompanion.map_size
import naksha.base.fn.Fn2
import kotlin.collections.MutableMap.MutableEntry
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/**
 * A map that is not thread-safe.
 */
@Suppress("NON_EXPORTABLE_TYPE")
@JsExport
open class MapProxy<K : Any, V : Any>(val keyKlass: KClass<out K>, val valueKlass: KClass<out V>) :
    Proxy(), MutableMap<K, V?> {

    override fun createData(): PlatformMap = Platform.newMap()
    override fun platformObject(): PlatformMap = super.platformObject() as PlatformMap

    override fun bind(data: PlatformObject, symbol: Symbol) {
        require(data is PlatformMap)
        super.bind(data, symbol)
    }

    /**
     * Helper to return the value stored for the key, if the key does not exist or the value is not of the expected type, the alternative is returned.
     * @param key the key to query.
     * @param alternative the alternative to return, when the value is not of the expected type.
     * @return the value.
     */
    fun <T : Any> getOr(key: K, alternative: T): T {
        val data = platformObject()
        val raw = map_get(data, key)
        val value = box(raw, Platform.klassOf(alternative))
        return value ?: alternative
    }

    /**
     * Helper to return the value of the key, if the key does not exist or the value is not of the expected type, the alternative is set
     * and returned.
     * @param key the key to query.
     * @param alternative the alternative to set and return, when the value is not of the expected type.
     * @return the value.
     */
    fun <T : Any> getOrSet(key: K, alternative: T): T {
        val data = platformObject()
        val raw = map_get(data, key)
        var value = box(raw, Platform.klassOf(alternative))
        if (value == null) {
            value = alternative
            map_set(data, key, unbox(value))
        }
        return value
    }

    /**
     * Helper to return the value of the key, if the key does not exist or is not of the expected type, a new value is created, stored
     * with the key and returned.
     * @param key the key to query.
     * @param klass the [KClass] of the expected value type.
     * @param init the initialize method to invoke, when the value is not of the expected type.
     * @return the value.
     */
    fun <T : Any, KEY : K, SELF : MapProxy<K, V>> getOrInit(
        key: KEY,
        klass: KClass<out T>,
        init: Fn2<out T, in SELF, in KEY>
    ): T {
        val data = platformObject()
        var value: T? = null
        if (map_contains_key(data, key)) {
            val raw = map_get(data, key)
            value = box(raw, klass)
        }
        if (value == null) {
            @Suppress("UNCHECKED_CAST")
            value = init.call(this as SELF, key)
            map_set(data, key, unbox(value))
        }
        return value
    }

    /**
     * Helper to return the value of the key, if the key does not exist or is not of the expected type, a new
     * value is created, stored with the key and returned.
     * @param key the key to query.
     * @param klass the [KClass] of the expected value.
     * @param init the initialize method to invoke, when the value is not of the expected type.
     * @return The value.
     */
    fun <T : Any, KEY : K, SELF : MapProxy<K, V>> getOrCreate(
        key: KEY,
        klass: KClass<out T>,
        init: Fn2<out T?, in SELF, in KEY>? = null
    ): T {
        val data = platformObject()
        var raw: Any? = null
        if (map_contains_key(data, key)) {
            raw = map_get(data, key)
            val value = box(raw, klass)
            if (value != null) return value
        }
        if (init != null) {
            @Suppress("UNCHECKED_CAST")
            val value = init.call(this as SELF, key)
            if (value != null) {
                map_set(data, key, unbox(value))
                return value
            }
        }
        val value: T?
        if (Platform.isAssignable(JsEnum::class, klass)) {
            @Suppress("UNCHECKED_CAST")
            value = JsEnum.get(raw, klass as KClass<out JsEnum>) as T
        } else {
            value = Platform.newInstanceOf(klass)
        }
        map_set(data, key, unbox(value))
        return value
    }

    /**
     * Helper to return the value of the key in the desired type. If the key does not exist, or is not of the expected type, `null` is returned.
     * @param <T> The expected type.
     * @param key The key to query.
     * @return The value as expected type or `null`, if no such key exists, or the value can't be proxied as the desired type.
     * @see [getOrNull]
     */
    fun <T : Any> getAs(key: K, klass: KClass<out T>): T? =
        box(map_get(platformObject(), key), klass)

    /**
     * Helper to return the value of the key in the desired type. If the key does not exist, or is not of the expected type, `null` is returned.
     * @param <T> The expected type.
     * @param key The key to query.
     * @return The value as expected type or `null`, if no such key exists, or the value can't be proxied as the desired type.
     * @see [getAs]
     */
    fun <T : Any> getOrNull(key: K, klass: KClass<out T>): T? =
        box(map_get(platformObject(), key), klass)

    /**
     * Convert the given value into a key.
     * @param value The value to convert.
     * @param alt The alternative to return when the value can't be cast.
     * @return The given value as key.
     */
    open fun toKey(value: Any?, alt: K? = null): K? = box(value, keyKlass, alt)

    /**
     * Convert the given value into a value.
     * @param key The key for which to convert the value.
     * @param value The value to convert.
     * @param alt The alternative to return when the value can't be cast.
     * @return The given value as value.
     */
    open fun toValue(key: K, value: Any?, alt: V? = null): V? = box(value, valueKlass, alt)

    override val size: Int
        get() = map_size(platformObject())

    override val entries: MutableSet<MutableEntry<K, V?>>
        get() {
            val platformObject = platformObject()
            val size = map_size(platformObject)
            val it = map_iterator(platformObject)
            var result: PlatformIteratorResult<PlatformList>? = null
            val entries = Array<MutableEntry<K, V?>>(size) { i ->
                result = it.next()
                val entry = result.value
                require(array_get_length(entry) == 2) { "Key $i can't be null" }
                val key = toKey(array_get(entry, 0))
                requireNotNull(key) { "Key $i can't be null" }
                val value = toValue(key, array_get(entry, 1))
                Entry(key, value, this)
            }
            return MapProxyEntrySet(entries.toMutableSet(), this)
        }

    override val keys: MutableSet<K>
        get() {
            val platformObject = platformObject()
            val size = map_size(platformObject)
            val it = map_iterator(platformObject)
            var result: PlatformIteratorResult<PlatformList>? = null
            val keys = Array(size) { i ->
                result = it.next()
                val entry = result.value
                require(array_get_length(entry) == 2) { "Key $i can't be null" }
                val key = toKey(array_get(entry, 0))
                requireNotNull(key) { "Key $i can't be null" }
                key as Any
            }
            @Suppress("UNCHECKED_CAST")
            return keys.toMutableSet() as MutableSet<K>
        }

    override val values: MutableCollection<V?>
        get() {
            val platformObject = platformObject()
            val size = map_size(platformObject)
            val it = map_iterator(platformObject)
            var result: PlatformIteratorResult<PlatformList>? = null
            val values = Array(size) { i ->
                result = it.next()
                val entry = result.value
                require(array_get_length(entry) == 2) { "Key $i can't be null" }
                val key = toKey(array_get(entry, 0))
                requireNotNull(key) { "Key $i can't be null" }
                val value = toValue(key, array_get(entry, 1))
                value as Any?
            }
            @Suppress("UNCHECKED_CAST")
            return values.toMutableList() as MutableCollection<V?>
        }

    override fun clear() = map_clear(platformObject())

    override fun isEmpty(): Boolean = map_size(platformObject()) == 0

    override fun remove(key: K): V? = toValue(key, map_remove(platformObject(), key))

    override fun putAll(from: Map<out K, V?>) {
        from.onEach { (key, value) -> put(key, value) }
    }

    fun addAll(vararg items: Any?) {
        val data = platformObject()
        var i = 0
        while (i < items.size) {
            val key = toKey(items[i++])
            val value = if (i < items.size) unbox(items[i++]) else null
            require(key != null)
            map_set(data, key, value)
        }
    }

    override fun put(key: K, value: V?): V? =
        toValue(key, map_set(platformObject(), key, unbox(value)))

    override fun get(key: K): V? = toValue(key, map_get(platformObject(), key))

    /**
     * Returns the value of the given _key_ as [Id]. If the value assigned to _key_ is a [String], the method converts it into an [Id] instance.
     * @param key the key from which to read the value.
     * @param update if _true_ and the value was converted into an [Id], the key is reassigned to the new [Id] instance so the same convert has not to be done again. Defaults to _false_.
     * @return the value assigned to the given _key_ as [Id] or `null`, if the value is no valid [Id].
     * @since 3.0
     */
    @JvmOverloads
    fun getId(key: K, update: Boolean = false): Id? {
        val v = map_get(platformObject(), key)
        if (v is Id) return v
        if (v !is String) return null
        val id = Id(v)
        if (update) map_set(platformObject(), id, v)
        return id
    }

    /**
     * Returns the raw value stored in the underlying base map.
     * @param key The key to read.
     * @return The raw value, being either a scalar or [PlatformObject].
     */
    fun getRaw(key: Any): Any? = map_get(platformObject(), key)

    /**
     * Sets the raw value stored in the underlying base map.
     * @param key The key to set.
     * @param value The value to set.
     * @return The previously set value.
     */
    fun setRaw(key: Any, value: Any?): Any? = map_set(platformObject(), key, unbox(value))

    /**
     * Tests if the underlying base map stored the given key.
     * @param key The key to test.
     * @return _true_ if the underlying map contains the given key; _false_ otherwise.
     */
    fun hasRaw(key: Any): Boolean = map_contains_key(platformObject(), key)

    /**
     * Removes the key from the underlying base map.
     * @param key The key to remove.
     * @return The value that was removed; _null_ if either the value was _null_ or no such key existed.
     */
    fun removeRaw(key: Any): Any? = map_remove(platformObject(), key)

    override fun containsValue(value: V?): Boolean = map_contains_value(platformObject(), value)

    override fun containsKey(key: K): Boolean = map_contains_key(platformObject(), key)

    class Entry<K : Any, V : Any>(
        override val key: K,
        initialValue: V?,
        private val owner: MapProxy<K, V>
    ) : MutableEntry<K, V?> {

        private var currentValue: V? = initialValue

        override fun setValue(newValue: V?): V? {
            val oldValue = currentValue
            currentValue = newValue
            owner.setRaw(key, newValue)
            return oldValue
        }

        override val value: V?
            get() = currentValue
    }

    class MapProxyEntrySet<K : Any, V : Any>(
        private val basicEntries: MutableSet<MutableEntry<K, V?>>,
        private val owner: MapProxy<K, V>
    ) : MutableSet<MutableEntry<K, V?>> by basicEntries {

        override fun iterator(): MutableIterator<MutableEntry<K, V?>> {
            return MapProxyEntriesIterator(basicEntries.iterator(), owner)
        }
    }

    class MapProxyEntriesIterator<K : Any, V : Any>(
        private val basicIterator: MutableIterator<MutableEntry<K, V?>>,
        private val owner: MapProxy<K, V>
    ) : MutableIterator<MutableEntry<K, V?>> by basicIterator {

        private var currentKey: K? = null

        override fun next(): MutableEntry<K, V?> {
            val next = basicIterator.next()
            currentKey = next.key
            return next
        }

        override fun remove() {
            owner.removeRaw(
                currentKey ?: throw IllegalStateException("Iterator is invalid position")
            )
        }
    }
}
