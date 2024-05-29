@file:Suppress("OPT_IN_USAGE")

package com.here.naksha.lib.base

import com.here.naksha.lib.base.N.Companion.unbox
import com.here.naksha.lib.base.N.Companion.undefined
import kotlin.js.JsExport
import kotlin.jvm.JvmStatic

@Suppress("MemberVisibilityCanBePrivate")
@JsExport
open class OldBasePairs<E>(vararg args: Any?) : OldBaseElementType<E>() {
    init {
        @Suppress("SENSELESS_COMPARISON")
        if (args !== null && args !== undefined && args.isNotEmpty()) {
            this.data = N.newObject(*args)
        }
    }

    companion object {
        @JvmStatic
        val klass = object : OldBasePairsKlass<Any?, OldBasePairs<Any?>>() {
            override fun isInstance(o: Any?): Boolean = o is OldBasePairs<*>

            override fun newInstance(vararg args: Any?): OldBasePairs<Any?> = OldBasePairs(*args)
        }
    }

    override fun klass(): OldBaseKlass<*> = klass

    override fun data(): N_Object {
        var data = this.data
        if (data == null) {
            data = N.newObject()
            this.data = data
        }
        return data as N_Object
    }

    /**
     * Returns the value of the key. If no such key exists or the value is not of the requested [Klass],
     * returns the given alternative.
     * @param key The key to query.
     * @param klass The type of the value.
     * @param alternative The alternative to return, when the current value is not of the requested [Klass].
     * @return The value.
     */
    protected open fun <T> getOr(key: String, klass: Klass<T>, alternative: T): T {
        if (data === null || data === undefined) return alternative
        val value = toElement(data()[key], klass, null)
        return if (value == null || value === undefined) alternative else value
    }

    /**
     * Returns the value of the key. If no such key exists or the value is not of the requested [Klass],
     * returns _null_.
     * @param key The key to query.
     * @param klass The type of the value.
     * @return The value or _null_.
     */
    protected open fun <T> getOrNull(key: String, klass: Klass<T>): T? {
        if (data === null || data === undefined) return null
        val value = toElement(data()[key], klass, null)
        return if (value == null || value === undefined) null else value
    }

    /**
     * Returns the value of the key. If no such key exists or the value is not of the requested [Klass],
     * creates a new value, assigns it and returns it.
     * @param key The key to query.
     * @param klass The type of the value.
     * @return The value.
     */
    protected open fun <T> getOrCreate(key: String, klass: Klass<T>, vararg args: Any?): T {
        val data = data()
        var value = toElement(data[key], klass, null)
        if (value == null) {
            value = klass.newInstance(*args)
            data[key] = unbox(value)
        }
        return value!!
    }

    protected open operator fun get(key: String): E? {
        val data = this.data
        if (data == null || data === undefined) return null
        return toElement(data()[key], this.componentKlass, null)
    }

    protected open operator fun set(key: String, value: E?): E? {
        val data = data()
        val old = toElement(data[key], this.componentKlass, null)
        data[key] = unbox(value)
        return old
    }
}