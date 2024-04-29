import com.here.naksha.lib.jbon.XYZ_OP_CREATE
import com.here.naksha.lib.jbon.XYZ_OP_UPDATE
import com.here.naksha.lib.jbon.XYZ_OP_UPSERT
import com.here.naksha.lib.jbon.XyzBuilder
import com.here.naksha.lib.jbon.asArray
import com.here.naksha.lib.jbon.asMap
import com.here.naksha.lib.jbon.get
import com.here.naksha.lib.jbon.newMap
import com.here.naksha.lib.jbon.put
import com.here.naksha.lib.plv8.JvmPlv8Table
import com.here.naksha.lib.plv8.NKC_DISABLE_HISTORY
import com.here.naksha.lib.plv8.NKC_PARTITION
import com.here.naksha.lib.plv8.NakshaSession
import com.here.naksha.lib.plv8.RET_ERR_MSG
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(Plv8TestContainer::class)
class NakshaSessionTest : JbTest() {

    private val collectionId = "foo_common"

    @Test
    fun testGetBaseCollectionId() {
        // given
        val session = NakshaSession.get()

        // expect
        assertEquals("foo", session.getBaseCollectionId("foo\$p7"))
    }

    @Test
    fun testEnsureHistoryPartition() {
        // given
        val session = NakshaSession.get()
        createCollection(session = session, collectionId = collectionId, partition = true, disableHistory = false)

        // then
        val collectionConfig = session.getCollectionConfig(collectionId)
        val isHistoryDisabled: Boolean = collectionConfig[NKC_DISABLE_HISTORY]!!
        assertFalse(isHistoryDisabled)
        val isPartitioningEnabled: Boolean = collectionConfig[NKC_PARTITION]!!
        assertTrue(isPartitioningEnabled)
        val expectedPartitionName = "${collectionId}\$hst_${session.txn().year()}"
        assertTrue(doesTableExist(session, expectedPartitionName))
    }

    @Test
    fun transactionShouldBeUpdatedWhenExecutingWriteFeaturesMultipleTimes() {
        // given
        val session = NakshaSession.get()
        createCollection(session = session, collectionId = collectionId, partition = true, disableHistory = false)
        val otherCollection = "collection2"
        createCollection(session = session, collectionId = otherCollection, partition = true, disableHistory = false)
        session.clear()

        val builder = XyzBuilder.create(65536)
        val op1 = builder.buildXyzOp(XYZ_OP_CREATE, "feature1")
        val op2 = builder.buildXyzOp(XYZ_OP_CREATE, "feature2")
        val op3 = builder.buildXyzOp(XYZ_OP_CREATE, "feature3")

        // when
        session.writeFeatures(collectionId, arrayOf(op1))

        // then
        assertEquals(1, session.transaction.modifiedFeatureCount)
        assertEquals(1, session.transaction.collectionCounters[collectionId])

        // when executed again in same session
        session.writeFeatures(collectionId, arrayOf(op2))
        session.writeFeatures(otherCollection, arrayOf(op3))

        // then
        assertEquals(3, session.transaction.modifiedFeatureCount)
        assertEquals(2, session.transaction.collectionCounters[collectionId])
        assertEquals(1, session.transaction.collectionCounters[otherCollection])
    }

    @Test
    fun writeFeaturesShouldNotAllowMultipleOperationsOnSameFeature() {
        // given
        val collectionId = "foo"
        val session = NakshaSession.get()
        session.collectionConfiguration.put(collectionId, newMap())

        val builder = XyzBuilder.create(65536)
        val op1 = builder.buildXyzOp(XYZ_OP_CREATE, "someId")
        val op2 = builder.buildXyzOp(XYZ_OP_UPDATE, "someId")

        // when
        val result = session.writeFeatures(collectionId, arrayOf(op1, op2)) as JvmPlv8Table

        // then
        val error: String? = result.rows[0][RET_ERR_MSG]
        assertEquals("Cannot perform multiple operations on single feature in one transaction", error)
    }

    private fun createCollection(session: NakshaSession, collectionId: String, partition: Boolean = false, disableHistory: Boolean = true) {
        val collectionJson = """{"id":"$collectionId","type":"NakshaCollection","maxAge":3560,"partition":$partition,"properties":{},"disableHistory":$disableHistory}"""
        val builder = XyzBuilder.create(65536)
        val op = builder.buildXyzOp(XYZ_OP_UPSERT, collectionId, null, 1111)
        val feature = builder.buildFeatureFromMap(asMap(env.parse(collectionJson)))
        session.writeCollections(arrayOf(op), arrayOf(feature), arrayOf(null), arrayOf(null), arrayOf(null))
    }

    private fun doesTableExist(session: NakshaSession, tableName: String): Boolean {
        val result = asArray(session.sql.execute("""SELECT EXISTS (
            SELECT FROM pg_tables WHERE tablename  = '$tableName'
            );"""))
        return asMap(result[0])["exists"]!!
    }
}