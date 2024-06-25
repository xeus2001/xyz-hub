package naksha.plv8.read

import naksha.model.Flags.GEO_TYPE_TWKB
import naksha.model.request.ReadCollections
import naksha.model.request.ReadFeatures
import naksha.model.request.ReadRequest
import naksha.model.request.condition.*
import naksha.model.request.condition.SOpType.INTERSECTS
import naksha.plv8.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
internal class ReadQueryBuilder(val sql: IPlv8Sql) {

    private val geometryTransformer = SqlGeometryTransformationResolver(sql)

    /**
     * Builds SQL request based on given ReadRequest.
     *
     * @param req - read request
     * @return <SQL query string, list of params for query>
     */
    fun build(req: ReadRequest): Pair<String, MutableList<Any?>> {
        return when (req) {
            is ReadCollections -> buildReadFeatures(req.toReadFeatures())
            is ReadFeatures -> buildReadFeatures(req)
            else -> throw NotImplementedError()
        }
    }

    private fun buildReadFeatures(req: ReadFeatures): Pair<String, MutableList<Any?>> {
        val queryBuilder = StringBuilder()
        val columnsToFetch = resolveColumns(req)

        val allQueriesParams = mutableListOf<Any?>()
        val quotedTablesToQuery = req.getQuotedTablesToQuery()

        queryBuilder.append("SELECT * FROM (")
        for (table in quotedTablesToQuery.withIndex()) {
            val whereSql = resolveFeaturesWhere(req, allQueriesParams)

            queryBuilder.append("\n(")
            queryBuilder.append("SELECT $columnsToFetch")
            queryBuilder.append(" FROM ${table.value}")
            if (whereSql.isNotEmpty()) {
                queryBuilder.append(" WHERE $whereSql")
            }
            queryBuilder.append(")")
            if (table.index < quotedTablesToQuery.size - 1) {
                queryBuilder.append("\nUNION ALL")
            }
        }
        queryBuilder.append("\n) LIMIT ${req.limit}")

        return Pair(queryBuilder.toString(), allQueriesParams)
    }

    /**
     * Returns SQL columns asked by user.
     *
     * @param req
     * @return comma delimited column names
     */
    private fun resolveColumns(req: ReadRequest): String {
        var columns = "$COL_ID, $COL_TYPE, $COL_GEO_REF, $COL_FLAGS"
        if (!req.noMeta) {
            columns += ", $COL_TXN_NEXT, $COL_TXN, $COL_UID, $COL_PTXN, $COL_PUID, $COL_ACTION, $COL_VERSION, $COL_CREATED_AT, $COL_UPDATE_AT, $COL_AUTHOR_TS, $COL_AUTHOR, $COL_APP_ID, $COL_GEO_GRID"
        }
        if (!req.noTags) {
            columns += ", $COL_TAGS"
        }
        if (!req.noGeometry) {
            columns += ", $COL_GEOMETRY"
        }
        if (!req.noFeature) {
            columns += ", $COL_FEATURE"
        }
        return columns
    }

    private fun resolveFeaturesWhere(req: ReadFeatures, allQueriesParams: MutableList<Any?>): String {
        val whereSql = StringBuilder()
        resolveOps(whereSql, allQueriesParams, req.op)

        return whereSql.toString()
    }

    /**
     * Prepares WHERE query using requested Ops.
     *
     * @param whereSql - sql to add new conditions to
     * @param paramsList - list of params to add conditions values to
     */
    private fun resolveOps(whereSql: StringBuilder, paramsList: MutableList<Any?>, op: Op?) {
        if (op == null) return

        when (op) {
            is LOp -> addLOp(whereSql, paramsList, op)
            is POp -> addPOp(whereSql, paramsList, op)
            is SOp -> addSop(whereSql, paramsList, op)
        }
    }

    /**
     * Recursively add logical operations between where conditions.
     * I.e.: (( ... AND ...) OR ...)
     *
     * @param whereSql - StringBuilder to add conditions
     * @param paramsList - list of params - will be modified whenever condition is compared to value provided by request.
     */
    private fun addLOp(whereSql: StringBuilder, paramsList: MutableList<Any?>, lop: LOp) {
        whereSql.append("(")
        for (el in lop.children.withIndex()) {
            resolveOps(whereSql, paramsList, el.value)
            if (el.index < lop.children.size - 1) {
                whereSql.append(" ${lop.op.operator} ")
            }
        }
        whereSql.append(")")
    }

    /**
     * Adds basic conditions like =, <, >, ≤, ≥, etc. to where statement.
     * I.e.:  id = $1
     *
     * @param whereSql - StringBuilder to add conditions
     * @param paramsList - list of params - will be modified whenever condition is compared to value provided by request.
     */
    private fun addPOp(whereSql: StringBuilder, paramsList: MutableList<Any?>, pop: POp) {
        // real column names
        val col = when (pop.propertyRef) {
            PRef.ID -> COL_ID
            PRef.APP_ID -> COL_APP_ID
            PRef.AUTHOR -> COL_AUTHOR
            PRef.UID -> COL_UID
            PRef.GRID -> COL_GEO_GRID
            PRef.TXN -> COL_TXN
            PRef.TXN_NEXT -> COL_TXN_NEXT
            PRef.TAGS -> COL_TAGS
            is PRef.NON_INDEXED_PREF -> (pop.propertyRef as PRef.NON_INDEXED_PREF).path.joinToString { "->" }
        }

        whereSql.append(col)
        whereSql.append(pop.op.operation)
        if (pop.value != null) {
            whereSql.append(paramsList.nextPlaceHolder())
            paramsList.add(pop.value)
        }
    }

    /**
     * Adds Spatial conditions to where section
     *
     * @param whereSql - StringBuilder to add conditions
     * @param paramsList - list of params - will be modified whenever condition is compared to value provided by request.
     * @param sop - requested spatial operations.
     */
    private fun addSop(whereSql: StringBuilder, paramsList: MutableList<Any?>, sop: SOp) {
        val valuePlaceholder = paramsList.nextPlaceHolder()
        when (sop.op) {
            INTERSECTS -> {
                val wrapperForReqValuePlaceholder = geometryTransformer.wrapWithTransformation(
                    sop.geometryTransformation,
                    "ST_Force3D(naksha_geometry_in_type($GEO_TYPE_TWKB::int2,$valuePlaceholder))"
                )
                whereSql.append("ST_Intersects(naksha_geometry(flags,geo), $wrapperForReqValuePlaceholder)")

                // TODO FIXME, sop.geometry should be transformed to byteArray once we have Platform <-> twkb converter
                paramsList.add(sop.geometry)
            }
        }
    }

    /**
     * Return all tables to query, if the request was for collection "foo" with history, then ["foo", "foo$hst"]
     * will be deleted
     *
     * @return table names to be queries
     */
    private fun ReadFeatures.getQuotedTablesToQuery(): List<String> {
        val tables = mutableListOf<String>()
        for (collection in collectionIds) {
            tables.add(sql.quoteIdent(collection))
            if (queryDeleted)
                tables.add(sql.quoteIdent("$collection\$del"))
            if (queryHistory)
                tables.add(sql.quoteIdent("$collection\$hst"))
        }
        return tables
    }

    private fun ReadCollections.toReadFeatures(): ReadFeatures {
        val op = if (this.ids.isNotEmpty()) {
            POp.isIn(PRef.ID, this.ids)
        } else null
        return ReadFeatures(
            collectionIds = arrayOf(NKC_TABLE),
            op = op,
            resultFilter = resultFilter,
            queryDeleted = queryDeleted,
            limit = this.limit,
            noFeature = noFeature,
            noMeta = noMeta,
            noTags = noTags,
            noGeometry = noGeometry
        )
    }

    private fun MutableList<*>.nextPlaceHolder() = "${"$"}${this.size + 1}"
}