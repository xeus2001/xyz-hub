@file:OptIn(ExperimentalJsExport::class)

package com.here.naksha.lib.jbon

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * The base class for all object mapper.
 */
@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "PropertyName")
@JsExport
abstract class JbObjectMapper<SELF : JbObjectMapper<SELF>> {
    /**
     * The reader used to read from the mapped view. This makes multiple readers from the same underlying view
     * independent, but they share the local dictionary.
     */
    val reader = JbReader()

    /**
     * The start of the currently mapped object.
     */
    var start: Int = 0

    /**
     * The start of the object content.
     */
    var encodingStart: Int = 0

    /**
     * The end of the object content.
     */
    var encodingEnd: Int = 0

    /**
     * Invoked after a view or reader was mapped to parse the header. If the invocation is mandatory, then the method must update
     * the [encodingStart] and [encodingEnd] from the header. When the method is called, the [reader] will be placed at [start] to
     * allow parsing the header. After returning, the [reader] will automatically be placed at [encodingStart].
     *
     * The caller guarantees to call the method after an optionally available local and/or global dictionary have been set.
     *
     * Eventually the method should call [setContent] or [setContentSize].
     *
     * @param mandatory If true, then parsing the header is required.
     */
    protected abstract fun parseHeader(mandatory: Boolean)

    /**
     * Can be invoked by [parseHeader] after the header is parsed and when the [reader] is located at the end of the header
     * and at the start of the content. Providing the object size as parameter (like read from the header), this method
     * calculates the [encodingStart] and [encodingEnd].
     * @param size The size as read from the object header.
     */
    protected fun setContentSize(size: Int) {
        val headerStart = start
        val headerEnd = reader.offset
        val headerSize = headerEnd - headerStart
        encodingStart = headerEnd
        encodingEnd = headerEnd + (size - headerSize)
    }

    /**
     * Can be invoked by [parseHeader] to set the detected content.
     * @param contentStart The start of the content.
     * @param contentEnd The end of the content, so the first byte that must **not** be read.
     */
    protected fun setContent(contentStart: Int, contentEnd: Int) {
        encodingStart = contentStart
        encodingEnd = contentEnd
    }

    /**
     * Can be invoked by [parseHeader] to signal that there is no content.
     */
    protected fun noContent() {
        encodingStart = reader.useView().getSize()
        encodingEnd = encodingStart
    }

    /**
     * Map a specific region of a view as object. If the [leadInOffset] and [contentStart] are equal, the only effect will be
     * that the wrong size is reported, decoding will still work fine (basically it means that the header is absent).
     *
     * @param view The view to map.
     * @param leadInOffset The offset where the object starts (lead-in byte of header).
     * @param contentStart The offset where the object content encoding starts (behind the header).
     * @param contentEnd The end of the object (the offset of the first byte that does not belong to the object).
     * @param localDict The local dictionary to map, if any.
     * @param globalDict The global dictionary to use, if any.
     * @return this.
     */
    protected open fun mapInternal(view: IDataView?, leadInOffset: Int, contentStart: Int, contentEnd: Int, localDict: JbDict?, globalDict: JbDict?): SELF {
        clear()
        reader.localDict = localDict
        reader.globalDict = globalDict
        if (view != null) {
            require(leadInOffset in 0..contentStart && contentStart <= contentEnd && contentEnd <= view.getSize())
            start = leadInOffset
            encodingStart = contentStart
            encodingEnd = contentEnd
            reader.view = view
            reader.offset = leadInOffset
            parseHeader(leadInOffset == contentStart && contentStart == contentEnd)
            check(start in 0..encodingStart && encodingStart <= encodingEnd && encodingEnd <= view.getSize())
            reader.offset = encodingStart
        }
        return this as SELF
    }

    /**
     * Returns the local dictionary or throws an [IllegalStateException].
     * @return The local dictionary.
     */
    fun localDict(): JbDict {
        val localDict = reader.localDict
        check(localDict != null)
        return localDict
    }

    /**
     * Clear the mapper, drops the view, the mapper becomes invalid.
     */
    open fun clear(): SELF {
        reader.view = null
        reader.offset = 0
        reader.localDict = null
        reader.globalDict = null
        start = 0
        encodingStart = 0
        encodingEnd = 0
        return this as SELF
    }

    /**
     * Resets the mapper to the start to parse the object again.
     * @return this.
     */
    open fun reset(): SELF {
        reader.offset = encodingStart
        return this as SELF
    }

    /**
     * Map a specific region of a view as object. If the [start] and [contentStart] are equal, the only effect will be
     * that the wrong size is reported, decoding will still work fine (basically it means that the header is absent).
     *
     * If [start], [contentStart] and [contentEnd] are all equal, then the header-parsing will be forced. Otherwise header
     * parsing is optional and need to be invoked manually, if wanted.
     *
     * @param view The view to map.
     * @param start The offset where the object starts (lead-in byte of header).
     * @param contentStart The offset where the object content encoding starts (behind the header).
     * @param contentEnd The end of the object (the offset of the first byte that does not belong to the object).
     * @param localDict The local dictionary to map, if any.
     * @param globalDict The global dictionary to use, if any.
     * @return this.
     */
    fun map(view: IDataView?, start: Int, contentStart: Int, contentEnd: Int, localDict: JbDict? = null, globalDict: JbDict? = null): SELF {
        mapInternal(view, start, contentStart, contentEnd, localDict, globalDict)
        return this as SELF
    }

    /**
     * When called, this method will parse the header of the object and then invoke [map], detecting the [encodingStart]
     * and [encodingEnd] from the header. May additionally do other header processing.
     * @param view The view to set.
     * @param start The offset of the lead-in byte of the object.
     * @param localDict The local dictionary to map, if any.
     * @param globalDict The global dictionary to use, if any.
     * @return this.
     */
    fun mapView(view: IDataView?, start: Int = 0, localDict: JbDict? = null, globalDict: JbDict? = null): SELF {
        mapInternal(view, start, start, start, localDict, globalDict)
        return this as SELF
    }

    /**
     * When called, this method will map the given byte-array, automatically creating a view for them, detecting the
     * [encodingStart] and [encodingEnd] from the header stored in the bytes. May additionally do other header processing.
     * @param bytes The bytes to map.
     * @param start The offset of the lead-in byte of the byte-array.
     * @param end The offset of the first byte not to map.
     * @return this.
     */
    fun mapBytes(bytes: ByteArray?, start: Int = 0, end: Int = bytes?.size ?: Int.MAX_VALUE): SELF {
        val view = if (bytes != null) Jb.env.newDataView(bytes, start, end) else null
        mapInternal(view, 0, 0, 0, null, null)
        return this as SELF
    }

    /**
     * Maps the view from the given reader. Verifies that the reader is at a lead-in byte and reads the header to
     * detect [encodingStart] and [encodingEnd].
     * @param reader The reader from which to use the view and offset.
     * @return this.
     */
    fun mapReader(reader: JbReader?): SELF {
        if (reader != null) {
            val offset = reader.offset
            mapInternal(reader.view, offset, offset, offset, reader.localDict, reader.globalDict)
        } else {
            mapInternal(null, 0, 0, 0, null, null)
        }
        return this as SELF
    }

    /**
     * Returns the size of the content (amount of byte).
     */
    fun contentSize(): Int {
        return encodingEnd - encodingStart
    }

    /**
     * Returns the size (amount of byte) being mapped totally (including the lead-in, header and optional local dictionary).
     */
    fun mapSize(): Int {
        return encodingEnd - start
    }

    /**
     * Tests whether this mapper has any mapping.
     * @return _true_ if this mapper has any mapping; _false_ otherwise.
     */
    fun isMapped() : Boolean = reader.view != null
}