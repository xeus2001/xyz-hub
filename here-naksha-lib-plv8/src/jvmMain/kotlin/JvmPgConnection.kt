package com.here.naksha.lib.plv8

import naksha.base.GZip
import naksha.base.Int64
import naksha.base.ObjectProxy
import naksha.jbon.*
import naksha.plv8.IPgConnection
import naksha.plv8.Param
import naksha.plv8.PgDbInfo
import java.io.Closeable
import java.sql.Connection

/**
 * Java JDBC binding to grant access to PostgresQL.
 */
@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
class JvmPgConnection(var conn: Connection?) : IPgConnection, Closeable {
    private var dbInfo: PgDbInfo? = null

    override fun info(): PgDbInfo {
        if (dbInfo == null) {
            dbInfo = PgDbInfo(this)
        }
        return dbInfo!!
    }

    override fun affectedRows(any: Any): Int? = if (any is Int) any else null

    override fun rows(any: Any): Array<ObjectProxy>? = if (any is Array<*>) {
        (any as Array<Any>).map { (it as ObjectProxy) }.toTypedArray()
    } else null

    override fun execute(sql: String, args: Array<Any?>?): Any {
        val conn = this.conn
        check(conn != null)
        if (args.isNullOrEmpty()) {
            val stmt = conn.createStatement()
            stmt.use {
                return if (stmt.execute(sql)) JvmPlv8ResultSet(stmt.resultSet).toArray() else stmt.updateCount
            }
        }
        val query = JvmPlv8SqlQuery(sql)
        val stmt = query.prepare(conn)
        stmt.use {
            if (!args.isNullOrEmpty()) query.bindArguments(stmt, args)
            return if (stmt.execute()) JvmPlv8ResultSet(stmt.resultSet).toArray() else stmt.updateCount
        }
    }

    override fun prepare(sql: String, typeNames: Array<String>?): naksha.plv8.IPgPlan {
        val conn = this.conn
        check(conn != null)
        return JvmPgPlan(JvmPlv8SqlQuery(sql), conn)
    }

    override fun close() {
        conn?.close()
        conn = null
    }

    override fun executeBatch(plan: naksha.plv8.IPgPlan, bulkParams: Array<Array<Param>>): IntArray {
        plan as JvmPgPlan
        for (singleQueryParams in bulkParams) {
            for (p in singleQueryParams) {
                when (p.type) {
                    SQL_BYTE_ARRAY -> plan.setBytes(p.idx, p.value as ByteArray?)
                    SQL_STRING -> plan.setString(p.idx, p.value as String?)
                    SQL_INT16 -> plan.setShort(p.idx, p.value as Short?)
                    SQL_INT32 -> plan.setInt(p.idx, p.value as Int?)
                    SQL_INT64 -> plan.setLong(p.idx, p.value as Int64?)
                }
            }
            plan.addBatch()
        }

        return plan.executeBatch()
    }

    override fun gzipCompress(raw: ByteArray): ByteArray = GZip.gzip(raw)

    override fun gzipDecompress(raw: ByteArray): ByteArray = GZip.gunzip(raw)
}