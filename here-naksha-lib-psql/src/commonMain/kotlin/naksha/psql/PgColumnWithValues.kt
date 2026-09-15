package naksha.psql

import naksha.base.Platform.PlatformCompanion.toJSON
import naksha.base.illegalArg
import naksha.model.objects.MemberType

/**
 * Rows as selected in [PgRows].
 * @since 3.0
 */
@Suppress("UNCHECKED_CAST", "ArrayInDataClass")
internal data class PgColumnWithValues(
    /**
     * The [PgRows] to which this is bound.
     * @since 3.0
     */
    val pgRows: PgRows,

    /**
     * The index in [PgRows.columns].
     * @since 3.0
     */
    val index: Int,

    /**
     * The database column.
     * @since 3.0
     */
    val pgColumn: PgColumn,

    /**
     * An optional alias, if the column is mapped to a different name in the result-set.
     * @since 3.0
     */
    val alias: String = pgColumn.name,

    /**
     * The values of the column for each row.
     * @since 3.0
     */
    internal var values: Array<Any?> = arrayOfNulls(pgRows.size),
) {

    /**
     * The size of the [values], so amount of row values for this column.
     * @since 3.0
     */
    var size: Int
        get() = pgRows.size
        set(value) {
            pgRows.size = value
        }

    /**
     * Returns the value from the given index or `null`.
     * @param i the index to query.
     * @return the value at the given index or `null`.
     * @since 3.0
     */
    operator fun get(i: Int) = if (i in 0 until size) values[i] else null

    /**
     * Sets the value at the given index. If the current [size] is less than `i`, increases the size, potentially adding `null` values.
     * @param i the index to set.
     * @param value the value to set.
     * @throws naksha.base.NakshaException with error [ILLEGAL_ARGUMENT][naksha.base.NakshaError.ILLEGAL_ARGUMENT] if the given index `i` is less than zero.
     * @since 3.0
     */
    operator fun set(i: Int, value: Any?) {
        if (i < 0) throw illegalArg("The given index $i is less than zero")
        if (i >= size) size = i + 1
        values[i] = value
    }

    /**
     * Returns all values of this column as array that can be feed into PostgresQL [UNNEST](https://www.postgresql.org/docs/18/functions-array.html) function. Requires a minor hack for [naksha.model.TagList].
     * @see PgRows.setRow
     * @see PgRows.values
     */
    fun toArray(): Array<Any?> = Array(size) {
        val value = values[it]
        when (pgColumn.memberType) {
            // We keep it internally as text[], but UNNEST requires only one value.
            // Therefore, we work around by serializing the array into a JSON array, then deserializing after UNNEST.
            // See: PgRows.decodedColumns
            MemberType.TAG_LIST -> if (value is Array<*>) toJSON(value) else null
            else -> value
        }
    }

    override fun toString(): String = alias
}
