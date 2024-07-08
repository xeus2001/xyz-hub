package naksha.psql

import naksha.base.JvmInt64
import naksha.base.ObjectProxy
import naksha.base.Platform
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Internal helper class to handle results as if they were returned by PLV8 engine.
 */
class PsqlCursor private constructor(var rs: ResultSet?, var affectedRows: Int) : PgCursor {
    constructor(result: Any) : this(if (result is ResultSet) result else null, if (result is Int) result else -1)
    constructor(rs: ResultSet) : this(rs, -1)
    constructor(affectedRows: Int) : this(null, affectedRows)

    private val columnCount: Int
    private val columnIndices: MutableMap<String, Int>?
    private val columnNames: Array<String>?
    private val columnTypes: Array<String>?
    private var isRow = false

    init {
        val rs = this.rs
        if (rs != null) {
            val columnCount = rs.metaData.columnCount
            val columnIndices = HashMap<String, Int>()
            // We hack this, because we do not want to run the loop twice!
            @Suppress("UNCHECKED_CAST")
            val columnNames = arrayOfNulls<String>(columnCount) as Array<String>

            @Suppress("UNCHECKED_CAST")
            val columnTypes = arrayOfNulls<String>(columnCount) as Array<String>
            val metaData = rs.metaData
            var i = 0
            while (i < columnCount) {
                val columnIndex = i + 1
                val columnName = metaData.getColumnLabel(columnIndex)
                columnNames[i] = columnName
                columnTypes[i] = metaData.getColumnTypeName(columnIndex)
                columnIndices[columnName] = columnIndex
                i++
            }
            this.columnCount = 0
            this.columnNames = columnNames
            this.columnTypes = columnTypes
            this.columnIndices = columnIndices
        } else {
            this.columnCount = 0
            this.columnNames = null
            this.columnTypes = null
            this.columnIndices = null
        }
    }

    private fun rs(): ResultSet {
        val rs = this.rs
        check(rs != null) { "The cursor is not positioned at a row" }
        return rs
    }

    /**
     * Returns the result-set and verifies that it is positioned above a row.
     * @return the result-set, positioned above a row.
     * @throws IllegalStateException if this cursor is not result-set or
     */
    private fun rsAtRow(): ResultSet {
        val rs = rs()
        check(isRow) { "The cursor is not positioned at a row" }
        return rs
    }

    private fun columnNames(): Array<String> =
        columnNames ?: throw IllegalStateException("Initialization error: Missing column names array")
    private fun columnTypes(): Array<String> =
        columnTypes ?: throw IllegalStateException("Initialization error: Missing column type array")
    private fun columnIndices(): MutableMap<String, Int> =
        columnIndices ?: throw IllegalStateException("Initialization error: Missing column indices map")

    /**
     * Returns the number of affected row when this is the cursor of an update query.
     * @return the number of affected row; -1 if the query was a SELECT or UPDATE with RETURNS.
     */
    override fun affectedRows(): Int = affectedRows

    /**
     * Move the cursor to the next row.
     * @return _true_ if the cursor is positioned at a row; _false_ is the cursor is behind the last row.
     */
    override fun next(): Boolean {
        isRow = rs?.next() ?: false
        return isRow
    }

    /**
     * Move the cursor to the next row.
     * @return this.
     */
    override fun fetch(): PgCursor {
        next()
        return this
    }

    /**
     * Tests if the cursor is currently positioned at a valid row.
     * @return _true_ if the cursor is positioned at a row; _false_ is the cursor is behind the last row or before the first.
     */
    override fun isRow(): Boolean = isRow

    /**
     * Returns the current row number. The first row is 1, the row before the first row is 0.
     * @return the current row number (0 to n, where 0 is before the first row).
     */
    override fun rowNumber(): Int = rs?.row ?: -1

    /**
     * Tests if the current row has the given colum.
     * @param name the name of the column
     * @return _true_ if the current row contains a column with the given name.
     * @throws IllegalStateException if the cursor is not positioned above a valid row, [isRow] returns _false_.
     */
    override fun contains(name: String): Boolean {
        rsAtRow()
        return columnIndices()[name] != null
    }

    private fun columnValue(index: Int, type: String, rs: ResultSet): Any? = when (type) {
        "null" -> null
        "text", "varchar", "character", "char", "json", "uuid", "inet", "cidr", "macaddr", "xml", "internal",
        "point", "line", "lseg", "box", "path", "polygon", "circle", "int4range", "int8range", "numrange",
        "tsrange", "tstzrange", "daterange" -> rs.getString(index)

        "smallint", "int2" -> rs.getShort(index).toInt()
        "integer", "int4", "xid4", "oid" -> rs.getInt(index)
        "bigint", "int8", "xid8" -> JvmInt64(rs.getLong(index))
        "real" -> rs.getFloat(index).toDouble()
        "double precision" -> rs.getDouble(index)
        "numeric" -> rs.getBigDecimal(index)
        "boolean" -> rs.getBoolean(index)
        "timestamp" -> JvmInt64(rs.getTimestamp(index).toInstant().toEpochMilli())
        "date" -> JvmInt64(rs.getDate(index).toInstant().toEpochMilli())
        "bytea" -> rs.getBytes(index)
        "jsonb" -> Platform.fromJSON(rs.getString(index))
        "array" -> rs.getArray(index)
        else -> rs.getObject(index)
    }

    /**
     * Returns the column value of the current row.
     * @param name the name of the column
     * @return the value of the column; _null_ if the value is _null_ or no such column exists.
     * @throws IllegalStateException if the cursor is not positioned above a valid row, [isRow] returns _false_.
     */
    override fun column(name: String): Any? {
        val rs = rsAtRow()
        val i = columnIndices()[name] ?: return null
        val type = columnTypes()[i]
        return columnValue(i, type, rs)
    }

    /**
     * Returns the column value of the current row.
     * @param name the name of the column
     * @return the value of the column.
     * @throws IllegalStateException if the cursor is not positioned above a valid row, [isRow] returns _false_.
     * @throws ClassCastException if casting the value failed.
     * @throws NullPointerException if the value is null.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(name: String): T = column(name) as T

    /**
     * Reads the current row into a proxy object.
     * @param klass the type of the proxy object to create.
     * @return the created proxy object.
     */
    override fun <T : ObjectProxy> map(klass: KClass<T>): T {
        val rs = rsAtRow()
        val columnNames = columnNames()
        val columnTypes = columnTypes()
        val row = Platform.newInstanceOf(klass)
        var i = 0
        while (i < columnNames.size) {
            val name = columnNames[i]
            val type = columnTypes[i]
            // See: https://www.postgresql.org/message-id/AANLkTinsk4rwT7v-751bwQkgTN1rkA=8uE-jk69nape-@mail.gmail.com
            val value = columnValue(++i, type, rs)
            row[name] = value
        }
        return row
    }

    override fun close() {
        val rs = this.rs
        this.rs = null
        rs?.close()
    }
}
