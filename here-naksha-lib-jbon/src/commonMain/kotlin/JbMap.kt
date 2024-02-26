@file:OptIn(ExperimentalJsExport::class)

package com.here.naksha.lib.jbon

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@Suppress("DuplicatedCode")
@JsExport
class JbMap : JbEntryArray<JbMap>() {
    override fun parseHeader(mandatory: Boolean) {
        if (mandatory) {
            check(reader.unitType() == TYPE_MAP)
            val unitSize = reader.unitSize()
            check(reader.enterUnit())
            setContentSize(unitSize)
        }
        valueReader.mapView(reader.view, reader.offset, reader.localDict, reader.globalDict)
        index = -1
        key = null
        length = if (contentSize() == 0) 0 else Int.MAX_VALUE
    }

    override fun nextEntry(): Boolean {
        if (reader.offset < encodingEnd) {
            // Seek over key and value.
            reader.nextUnit()
            reader.nextUnit()
            return reader.offset < encodingEnd
        }
        return false
    }

    override fun loadEntry() {
        val reader = this.reader
        if (reader.offset != cachedOffset) {
            val vr = this.valueReader
            vr.setOffset(reader.offset)
            check(vr.isRef())
            val index = vr.readRef()
            val dict = if (vr.isGlobalRef()) vr.globalDict else vr.localDict
            check(dict != null)
            key = dict.get(index)
            vr.nextUnit()
            // We're now positioned at the value.
        }
    }

    override fun dropEntry() {
        cachedOffset = -1
        key = null
    }

    /**
     * A reader we use flexible, when reading of values is requested.
     */
    private val valueReader = JbReader()

    /**
     * The [reader] offset that currently is cached.
     */
    private var cachedOffset: Int = -1

    /**
     * The cached key at the current index, if index is valid.
     */
    private var key: String? = null

    fun key(): String {
        check(index >= 0)
        loadEntry()
        val key = this.key
        check(key != null)
        return key
    }

    fun value(): JbReader {
        check(index >= 0)
        loadEntry()
        return valueReader
    }

    /**
     * Searches the map for the given key and if found, select the entry with this key as current position.
     * @param key The key to search for.
     */
    fun selectKey(key: String): Boolean {
        val backup = reader.offset
        if (first()) {
            if (key == key()) return true
            while (next()) {
                if (key == key()) return true
            }
        }
        reader.offset = backup
        return false
    }

    /**
     * Returns this map as [IMap].
     * @return This binary as [IMap].
     */
    fun toIMap() : IMap {
        return JbReader.readMap(this)
    }
}