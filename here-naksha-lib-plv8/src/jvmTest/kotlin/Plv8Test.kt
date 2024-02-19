import com.here.naksha.lib.jbon.*
import com.here.naksha.lib.plv8.NakshaSession
import com.here.naksha.lib.plv8.Static
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class Plv8Test : Plv8TestContainer() {
    @Order(1)
    @Test
    fun selectJbonModule() {
        val session = NakshaSession.get()
        val plan = session.sql.prepare("SELECT * FROM commonjs2_modules WHERE module = $1", arrayOf(SQL_STRING))
        try {
            val cursor = plan.cursor(arrayOf("naksha"))
            try {
                var row = cursor.fetch()
                Assertions.assertNotNull(row)
                while (row != null) {
                    check(row is HashMap<*, *>)
                    Jb.log.info("row: ", row)
                    row = cursor.fetch()
                }
            } finally {
                cursor.close()
            }
        } finally {
            plan.free()
        }
    }

    @Order(2)
    @Test
    fun queryVersion() {
        val session = NakshaSession.get()
        val result = session.sql.execute("select naksha_version() as version")
        assertNull(session.sql.affectedRows(result))
        val rows = assertIs<Array<JvmMap>>(session.sql.rows(result))
        for (row in rows) {
            assertEquals(1, row.size)
            assertEquals(BigInt64(0L), row["version"])
            assertEquals(1, Jb.map.size(row))
            assertEquals(BigInt64(0L), Jb.map.get(row, "version"))
        }
    }

    @Order(4)
    @Suppress("LocalVariableName")
    @Test
    fun testVersion() {
        val v1_0_0 = XyzVersion(1, 0, 0)
        assertEquals(XyzVersion(1, 0, 0), XyzVersion.fromString("1.0.0"))
        assertEquals(XyzVersion(1, 0, 0), XyzVersion.fromString("1.0"))
        assertEquals(XyzVersion(1, 0, 0), XyzVersion.fromString("1"))
        assertEquals(v1_0_0, XyzVersion.fromBigInt(BigInt64(1) shl 32))
        assertEquals(v1_0_0.toBigInt(), BigInt64(1) shl 32)
        val v1_2_3 = XyzVersion(1, 2, 3)
        assertEquals("1.2.3", v1_2_3.toString())
        assertEquals(v1_2_3, XyzVersion.fromString("1.2.3"))
        assertTrue(v1_0_0 < v1_2_3)
    }

    @Order(5)
    @Test
    fun testDbVersion() {
        val session = NakshaSession.get()
        val version = session.postgresVersion()
        assertTrue(version >= XyzVersion(14, 0, 0))
    }

    @Order(6)
    @Test
    fun testPartitionNumbering() {
        var i = 0
        while (i++ < 10_000) {
            val s = env.randomString(12)
            val pnum = Static.partitionNumber(s)
            assertTrue(pnum in 0..255)
            val pid = Static.partitionNameForId(s)
            assertEquals(3, pid.length)
            val expectedId = if (pnum < 10) "00$pnum" else if (pnum < 100) "0$pnum" else "$pnum"
            assertEquals(expectedId, pid)
        }
    }

    @Order(7)
    @Test
    fun testTransactionNumber() {
        val session = NakshaSession.get()
        assertNotNull(session.txn())
    }

    @Order(8)
    @Test
    fun dropTestCollectionIfExists() {
        val session = NakshaSession.get()
        Static.collectionDrop(session.sql, "foo")
        session.sql.execute("COMMIT")
    }

    @Order(9)
    @Test
    fun createTestCollection() {
        val session = NakshaSession.get()
        Static.collectionCreate(session.sql,"foo", spGist = false, partition = false)
        session.sql.execute("COMMIT")
    }
}