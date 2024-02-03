import com.here.naksha.lib.jbon.JbSession
import com.here.naksha.lib.jbon.SQL_STRING
import com.here.naksha.lib.plv8.NakshaSession
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class Plv8Test : Plv8TestContainer() {
    @Order(1)
    @Test
    fun selectJbonModule() {
        val log = JbSession.log!!
        val session = NakshaSession.get()
        val plan = session.sql.prepare("SELECT * FROM commonjs2_modules WHERE module = $1", arrayOf(SQL_STRING))
        try {
            val cursor = plan.cursor(arrayOf("naksha"))
            try {
                var row = cursor.fetch()
                Assertions.assertNotNull(row)
                while (row != null) {
                    check(row is HashMap<*, *>)
                    log.info("row: ", row)
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
        val map = JbSession.map!!
        val result = session.sql.execute("select naksha_version() as version")
        assertNull(session.sql.affectedRows(result))
        val rows = assertIs<Array<HashMap<String,Any?>>>(session.sql.rows(result))
        for (row in rows) {
            assertEquals(1, row.size)
            assertEquals(0L, row["version"])
            assertEquals(1, map.size(row))
            assertEquals(0L, map.get(row, "version"))
        }
    }
}