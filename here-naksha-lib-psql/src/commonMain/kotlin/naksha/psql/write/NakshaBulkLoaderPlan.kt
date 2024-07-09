package naksha.psql.write

import naksha.jbon.*
import naksha.model.*
import naksha.model.XYZ_EXEC_CREATED
import naksha.model.XYZ_EXEC_DELETED
import naksha.model.XYZ_EXEC_PURGED
import naksha.model.XYZ_EXEC_RETAINED
import naksha.model.request.ResultRow
import naksha.model.Metadata
import naksha.model.Row
import naksha.psql.*
import naksha.psql.COL_ALL
import naksha.psql.COL_ALL_TYPES
import naksha.psql.COL_APP_ID
import naksha.psql.COL_AUTHOR
import naksha.psql.COL_AUTHOR_TS
import naksha.psql.COL_CREATED_AT
import naksha.psql.COL_FEATURE
import naksha.psql.COL_FLAGS
import naksha.psql.COL_GEOMETRY
import naksha.psql.COL_GEO_GRID
import naksha.psql.COL_GEO_REF
import naksha.psql.COL_ID
import naksha.psql.COL_PTXN
import naksha.psql.COL_PUID
import naksha.psql.COL_TAGS
import naksha.psql.COL_TXN
import naksha.psql.COL_TXN_NEXT
import naksha.psql.COL_TYPE
import naksha.psql.COL_UID
import naksha.psql.COL_UPDATE_AT
import naksha.psql.COL_VERSION
import kotlin.reflect.KFunction0

internal class NakshaBulkLoaderPlan(
    val collectionId: String,
    val partitionHeadQuoted: String,
    val session: NakshaSession,
    val isHistoryDisabled: Boolean?,
    val autoPurge: Boolean,
    val minResult: Boolean
) {

    private val headCollectionId = session.getBaseCollectionId(collectionId)
    private val delCollectionId = "${headCollectionId}\$del"
    private val delCollectionIdQuoted = PgUtil.quoteIdent(delCollectionId)
    private val hstCollectionIdQuoted = PgUtil.quoteIdent("${headCollectionId}\$hst")

    internal val featureIdsToDeleteFromDel = mutableListOf<String>()
    internal val featuresToPurgeFromDel = mutableListOf<String>()
    internal val result = mutableListOf<ResultRow>()

    internal val insertToHeadBulkParams = mutableListOf<Array<Param>>()
    internal val copyHeadToDelBulkParams = mutableListOf<Array<Param>>()
    internal val insertDelToHstBulkParams = mutableListOf<Array<Param>>()
    internal val updateHeadBulkParams = mutableListOf<Array<Param>>()
    internal val deleteHeadBulkParams = mutableListOf<Array<Param>>()
    internal val copyHeadToHstBulkParams = mutableListOf<Array<Param>>()

    private fun insertHeadPlan(): PgPlan {
        return session.usePgConnection().prepare(
            """INSERT INTO $partitionHeadQuoted (
                $COL_CREATED_AT,$COL_UPDATE_AT,$COL_TXN,$COL_UID,$COL_GEO_GRID,$COL_FLAGS,
                $COL_APP_ID,$COL_AUTHOR,$COL_TYPE,$COL_ID,
                $COL_FEATURE,$COL_TAGS,$COL_GEOMETRY,$COL_GEO_REF)
                VALUES($1,$2,$3,$4,$5,
                $6,$7,$8,$9,
                $10,$11,$12,$13,$14)""".trimIndent(),
            arrayOf(
                SQL_INT64, SQL_INT64, SQL_INT64, SQL_INT32, SQL_INT32, SQL_INT32,
                SQL_STRING, SQL_STRING, SQL_STRING, SQL_STRING,
                SQL_BYTE_ARRAY, SQL_BYTE_ARRAY, SQL_BYTE_ARRAY, SQL_BYTE_ARRAY
            )
        )
    }

    private fun updateHeadPlan(): PgPlan {
        return session.usePgConnection().prepare(
            """
                UPDATE $partitionHeadQuoted 
                SET $COL_TXN_NEXT=$1, $COL_TXN=$2, $COL_UID=$3, $COL_PTXN=$4,$COL_PUID=$5,$COL_FLAGS=$6,$COL_VERSION=$7,$COL_CREATED_AT=$8,$COL_UPDATE_AT=$9,$COL_AUTHOR_TS=$10,$COL_AUTHOR=$11,$COL_APP_ID=$12,$COL_GEO_GRID=$13,$COL_ID=$14,$COL_TAGS=$15,$COL_GEOMETRY=$16,$COL_FEATURE=$17,$COL_GEO_REF=$18,$COL_TYPE=$19 WHERE $COL_ID=$20
                """.trimIndent(),
            arrayOf(*COL_ALL_TYPES, SQL_STRING)
        )
    }

    private fun deleteHeadPlan(): PgPlan {
        return session.usePgConnection().prepare(
            """
                DELETE FROM $partitionHeadQuoted
                WHERE $COL_ID = $1
                """.trimIndent(),
            arrayOf(SQL_STRING)
        )
    }

    private fun insertDelPlan(): PgPlan {
        // ptxn + puid = txn + uid (as we generate new state in _del)
        return session.usePgConnection().prepare(
            """
                INSERT INTO $delCollectionIdQuoted ($COL_ALL) 
                SELECT $1,$2,$3,$COL_TXN,$COL_UID,$COL_FLAGS,$4,$5,$6,$7,$8,$9,$COL_GEO_GRID,$COL_ID,$COL_TAGS,$COL_GEOMETRY,$COL_FEATURE,$COL_GEO_REF,$COL_TYPE,$COL_FNVA1 
                    FROM $partitionHeadQuoted WHERE $COL_ID = $10""".trimIndent(),
            arrayOf(
                SQL_INT64, SQL_INT64, SQL_INT32, SQL_INT32,
                SQL_INT64, SQL_INT64, SQL_INT64, SQL_STRING, SQL_STRING, SQL_STRING
            )
        )
    }

    private fun insertDelToHstPlan(): PgPlan {
        return session.usePgConnection().prepare(
            """
                INSERT INTO $hstCollectionIdQuoted ($COL_ALL) 
                SELECT $1,$2,$3,$COL_TXN,$COL_UID,$COL_FLAGS,$4,$5,$6,$7,$8,$9,$10,$COL_GEO_GRID,$COL_ID,$COL_TAGS,$COL_GEOMETRY,$COL_FEATURE,$COL_GEO_REF,$COL_TYPE,$COL_FNVA1 
                    FROM $partitionHeadQuoted WHERE $COL_ID = $11""".trimIndent(),
            arrayOf(
                SQL_INT64, SQL_INT64, SQL_INT32, SQL_INT16, SQL_INT32, SQL_INT64,
                SQL_INT64, SQL_INT64, SQL_STRING, SQL_STRING, SQL_STRING
            )
        )
    }

    private fun copyHeadToHstPlan(): PgPlan {
        return session.usePgConnection().prepare(
            """
            INSERT INTO $hstCollectionIdQuoted ($COL_ALL) 
            SELECT $1,$COL_TXN,$COL_UID,$COL_PTXN,$COL_PUID,$COL_FLAGS,$COL_VERSION,$COL_CREATED_AT,$COL_UPDATE_AT,$COL_AUTHOR_TS,$COL_AUTHOR,$COL_APP_ID,$COL_GEO_GRID,$COL_ID,$COL_TAGS,$COL_GEOMETRY,$COL_FEATURE,$COL_GEO_REF,$COL_TYPE,$COL_FNVA1 
                FROM $partitionHeadQuoted WHERE $COL_ID = $2
            """.trimIndent(), arrayOf(SQL_INT64, SQL_STRING)
        )
    }

    fun addCreate(op: NakshaRequestOp) {
        val dbRow = op.dbRow!!
        addToRemoveFromDel(op.id)
        session.rowUpdater.xyzInsert(op.collectionId, dbRow)
        addInsertParams(dbRow)
        if (!minResult) {
            addResult(XYZ_EXEC_CREATED, dbRow)
        }
    }

    fun addUpdate(op: NakshaRequestOp, existingFeature: Row?) {
        val dbRow = op.dbRow!!
        val headBeforeUpdate: Row = existingFeature!!
        checkStateForAtomicOp(op.atomicUUID, headBeforeUpdate)

        addToRemoveFromDel(op.id)
        addCopyHeadToHstParams(op.id, isHistoryDisabled)
        session.rowUpdater.xyzUpdateHead(op.collectionId, dbRow, headBeforeUpdate)
        addUpdateHeadParams(op.dbRow)
        if (!minResult) {
            addResult(naksha.model.XYZ_EXEC_UPDATED, dbRow)
        }
    }

    private fun addToRemoveFromDel(id: String) {
        if (!autoPurge) // it's not in $del, so we can skip query
            featureIdsToDeleteFromDel.add(id)
    }

    fun addDelete(op: NakshaRequestOp, existingFeature: Row?) {
        addDeleteInternal(op, existingFeature)
        if (!minResult) {
            if (existingFeature == null) {
                addResult(XYZ_EXEC_RETAINED)
            } else {
                addResult(XYZ_EXEC_DELETED, existingFeature)
            }
        }
    }

    fun addPurge(op: NakshaRequestOp, existingFeature: Row?, existingInDelFeature: Row?) {
        addDeleteInternal(op, existingFeature)
        val deletedFeatureRow: Row? = existingInDelFeature ?: existingFeature
        checkStateForAtomicOp(op.atomicUUID, deletedFeatureRow)
        if (!autoPurge) // it's not in $del, so we don't have to purge
            featuresToPurgeFromDel.add(op.id)
        if (!minResult) {
            if (deletedFeatureRow == null) {
                addResult(XYZ_EXEC_RETAINED)
            } else {
                addResult(XYZ_EXEC_PURGED, deletedFeatureRow)
            }
        }
    }

    internal fun executeAll() {
        // 1.
        executeBatchDeleteFromDel(featureIdsToDeleteFromDel)
        // 3.
        executeBatch(::insertDelPlan, copyHeadToDelBulkParams)
        // 4. insert to history and update head
        executeBatch(::copyHeadToHstPlan, copyHeadToHstBulkParams)
        executeBatch(::updateHeadPlan, updateHeadBulkParams)
        // 5. copy head (del state) to hst
        executeBatch(::insertDelToHstPlan, insertDelToHstBulkParams)
        executeBatch(::deleteHeadPlan, deleteHeadBulkParams)
        // 6.
        executeBatch(::insertHeadPlan, insertToHeadBulkParams)
        // 7. purge
        executeBatchDeleteFromDel(featuresToPurgeFromDel)
    }

    private fun addDeleteInternal(op: NakshaRequestOp, existingFeature: Row?) {
        if (existingFeature != null) {
            // this may throw exception (if we try to delete non-existing feature - that was deleted before)
            val headBeforeDelete: Row = existingFeature
            checkStateForAtomicOp(op.atomicUUID, headBeforeDelete)
            addCopyHeadToHstParams(op.id, isHistoryDisabled)

            val newMeta = op.dbRow!!.meta!!.copy(
                version = headBeforeDelete.meta!!.version,
                authorTs = headBeforeDelete.meta!!.authorTs,
                updatedAt = headBeforeDelete.meta!!.updatedAt,
                createdAt = headBeforeDelete.meta!!.createdAt
            )
            session.rowUpdater.xyzDel(newMeta)
            if (!autoPurge) // do not push to del
                addDelParams(copyHeadToDelBulkParams, newMeta)
            addDeleteHeadParams(op.id)
            if (isHistoryDisabled == false) // even if it's autoPurge we still need a deleted copy in $hst if it's enabled.
                addDelParams(insertDelToHstBulkParams, newMeta)
        }
    }

    private fun addInsertParams(row: Row) {
        val meta = row.meta!!
        insertToHeadBulkParams.add(
            arrayOf(
                Param(1, SQL_INT64, meta.createdAt),
                Param(2, SQL_INT64, meta.updatedAt),
                Param(3, SQL_INT64, meta.txn),
                Param(4, SQL_INT32, meta.uid),
                Param(5, SQL_INT32, meta.geoGrid),
                Param(6, SQL_INT32, meta.flags),
                Param(7, SQL_STRING, meta.appId),
                Param(8, SQL_STRING, meta.author),
                Param(9, SQL_STRING, row.type),
                Param(10, SQL_STRING, row.id),
                Param(11, SQL_BYTE_ARRAY, row.feature),
                Param(12, SQL_BYTE_ARRAY, row.tags),
                Param(13, SQL_BYTE_ARRAY, row.geo),
                Param(14, SQL_BYTE_ARRAY, row.geoRef),
            )
        )
    }

    private fun addDelParams(params: MutableList<Array<Param>>, newMeta: Metadata) {
        params.add(
            arrayOf(
                Param(1, SQL_INT64, newMeta.txnNext),
                Param(2, SQL_INT64, newMeta.txn),
                Param(3, SQL_INT32, newMeta.uid),
                Param(4, SQL_INT32, newMeta.version),
                Param(5, SQL_INT64, newMeta.createdAt),
                Param(6, SQL_INT64, newMeta.updatedAt),
                Param(7, SQL_INT64, newMeta.authorTs),
                Param(8, SQL_STRING, newMeta.author),
                Param(9, SQL_STRING, newMeta.appId),
                Param(10, SQL_STRING, newMeta.id)
            )
        )
    }

    private fun addUpdateHeadParams(row: Row) {
        val meta = row.meta!!
        updateHeadBulkParams.add(
            arrayOf(
                Param(1, SQL_INT64, meta.txnNext),
                Param(2, SQL_INT64, meta.txn),
                Param(3, SQL_INT32, meta.uid),
                Param(4, SQL_INT64, meta.ptxn),
                Param(5, SQL_INT32, meta.puid),
                Param(6, SQL_INT32, meta.flags),
                Param(7, SQL_INT32, meta.version),
                Param(8, SQL_INT64, meta.createdAt),
                Param(9, SQL_INT64, meta.updatedAt),
                Param(10, SQL_INT64, meta.authorTs),
                Param(11, SQL_STRING, meta.author),
                Param(12, SQL_STRING, meta.appId),
                Param(13, SQL_INT32, meta.geoGrid),
                Param(14, SQL_STRING, row.id),
                Param(15, SQL_BYTE_ARRAY, row.tags),
                Param(16, SQL_BYTE_ARRAY, row.geo),
                Param(17, SQL_BYTE_ARRAY, row.feature),
                Param(18, SQL_BYTE_ARRAY, row.geoRef),
                Param(19, SQL_STRING, row.type),
                Param(20, SQL_STRING, row.id)
            )
        )
    }

    private fun addDeleteHeadParams(id: String) {
        deleteHeadBulkParams.add(
            arrayOf(
                Param(1, SQL_STRING, id)
            )
        )
    }

    private fun addCopyHeadToHstParams(id: String, isHstDisabled: Boolean?) {
        if (isHstDisabled == false) {
            copyHeadToHstBulkParams.add(
                arrayOf(
                    Param(1, SQL_INT64, session.txn().value),
                    Param(2, SQL_STRING, id),
                )
            )
        }
    }

    internal fun executeBatchDeleteFromDel(featureIdsToDeleteFromDel: MutableList<String>) {
        if (featureIdsToDeleteFromDel.isNotEmpty()) {
            session.usePgConnection().execute(
                "DELETE FROM  $delCollectionIdQuoted WHERE id = ANY($1)",
                arrayOf(featureIdsToDeleteFromDel.toTypedArray())
            )
        }
    }

    internal fun executeBatch(stmt: KFunction0<PgPlan>, bulkParams: List<Array<Param>>) {
        if (bulkParams.isNotEmpty()) {
            val statement = stmt()
            statement.addBatch(bulkParams.toTypedArray())
            val result = statement.executeBatch()
            if (result.isNotEmpty() && result[0] == -3) {
                // java.sql.Statement.EXECUTE_FAILED
                throw NakshaException.forBulk(ERR_FATAL, "error in bulk statement")
            }
        }
    }

    private fun checkStateForAtomicOp(reqUuid: String?, currentHead: Row?) {
        if (reqUuid != null) {
            check(currentHead != null)
            val headUuid =
                Guid(session.storage.id(), collectionId, currentHead.id, Luid(Txn(currentHead.meta!!.txn), currentHead.meta!!.uid)).toString()
            if (reqUuid != headUuid) {
                throw NakshaException.forId(
                    ERR_CHECK_VIOLATION,
                    "Atomic operation for $reqUuid is impossible, expected state: $reqUuid, actual: $headUuid",
                    reqUuid
                )
            }
        }
    }

    private fun addResult(op: String, row: Row? = null) {
        val resultRow = ResultRow(row = row, op = op)
        result.add(resultRow)
    }
}