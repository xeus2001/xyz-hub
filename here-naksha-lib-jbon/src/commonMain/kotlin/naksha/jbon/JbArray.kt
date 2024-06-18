package naksha.jbon

import naksha.base.Binary
import naksha.base.BinaryView
import kotlin.js.JsExport

/**
 * An array implementation.
 * @constructor Create a new JBON array reader.
 */
@Suppress("OPT_IN_USAGE")
@JsExport
class JbArray : JbEntryArray<JbArray>() {

    override fun parseHeader() {
        check(unitType == TYPE_ARRAY) { "Mapped structure is no array, but ${JbReader.unitTypeName(unitType)}" }
        index = -1
        length = if (bodySize() == 0) 0 else Int.MAX_VALUE
    }

    override fun nextEntry(): Boolean {
        if (reader.pos < end) {
            reader.nextUnit()
            return reader.pos < end
        }
        return false
    }

    override fun loadEntry() {
    }

    override fun dropEntry() {
    }

    /**
     * Returns the reader, being positioned at the value of the entry.
     * @return The reader, positioned at the value of the entry.
     * @throws IllegalStateException If the position is invalid.
     */
    fun value(): JbReader {
        check(index >= 0)
        return reader
    }

}