import com.here.naksha.lib.jbon.*
import com.here.naksha.lib.plv8.*
import com.here.naksha.lib.plv8.GEO_TYPE_NULL
import com.here.naksha.lib.plv8.RET_OP
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals

@Suppress("ArrayInDataClass")
class Plv8PerfTest : Plv8TestContainer() {

    data class Features(
            val size: Int,
            val idArr: Array<String?>,
            val opArr: Array<ByteArray?>,
            val featureArr: Array<ByteArray?>,
            val geoTypeArr: Array<Short>,
            val geoArr: Array<ByteArray?>,
            val tagsArr: Array<ByteArray?>
    )

    companion object {
        private val FeaturesPerRound = 1000
        private val Rounds = 10
        private val BulkSize = 50_000
        private val BulkThreads = Runtime.getRuntime().availableProcessors() * 2

        private val topologyJson = Plv8PerfTest::class.java.getResource("/topology.json")!!.readText(StandardCharsets.UTF_8)
        internal var topologyTemplate : IMap? = null
        private lateinit var jvmSql: JvmPlv8Sql
        private lateinit var conn: Connection
        private lateinit var session: NakshaSession
        private var baseLine: Double = 0.0

        @BeforeAll
        @JvmStatic
        fun prepare() {
            session = NakshaSession.get()
            jvmSql = session.sql as JvmPlv8Sql
            val conn = jvmSql.conn
            check(conn != null)
            this.conn = conn
        }
    }

    private fun currentMicros(): Long = System.nanoTime() / 1000

    private fun createFeatures(size: Int = 2000): Features {
        val builder = XyzBuilder.create(65536)
        val topology = asMap(env.parse(topologyJson))
        val idArr = Array<String?>(size) { null }
        val opArr = Array<ByteArray?>(size) { null }
        val featureArr = Array<ByteArray?>(size) { null }
        val geoTypeArr = Array(size) { GEO_TYPE_NULL }
        val geoArr = Array<ByteArray?>(size) { null }
        val tagsArr = Array<ByteArray?>(size) { null }
        var i = 0
        while (i < size) {
            val id: String = env.randomString(12)
            topology["id"] = id
            idArr[i] = id
            opArr[i] = builder.buildXyzOp(XYZ_OP_CREATE, id, null, "vgrid")
            featureArr[i] = builder.buildFeatureFromMap(topology)
            i++
        }
        return Features(size, idArr, opArr, featureArr, geoTypeArr, geoArr, tagsArr)
    }

    @Order(1)
    @Test
    fun createBaseline() {
        var stmt = conn.prepareStatement("""DROP TABLE IF EXISTS ptest;
CREATE TABLE ptest (uid int8, txn_next int8, geo_type int2, id text, xyz bytea, tags bytea, feature bytea, geo bytea);
""")
        stmt.use {
            stmt.executeUpdate()
        }
        val features = createFeatures(FeaturesPerRound)
        val start = currentMicros()
        stmt = conn.prepareStatement("INSERT INTO ptest (id, feature) VALUES (?, ?)")
        stmt.use {
            var i = 0
            while (i < features.size) {
                stmt.setString(1, features.idArr[i])
                stmt.setBytes(2, features.featureArr[i])
                stmt.addBatch()
                i++
            }
            stmt.executeBatch()
        }
        val end = currentMicros()
        conn.commit()
        baseLine = printStatistics(features.size, 1, end - start)
    }

    @Order(2)
    @Test
    fun insertFeatures() {
        val session = NakshaSession.get()
        val builder = XyzBuilder.create(65536)

        var op = builder.buildXyzOp(XYZ_OP_DELETE, "v2_perf_test", null, "vgrid")
        var feature = builder.buildFeatureFromMap(asMap(env.parse("""{"id":"v2_perf_test"}""")))
        var result = session.writeCollections(arrayOf(op), arrayOf(feature), arrayOf(GEO_TYPE_NULL), arrayOf(null), arrayOf(null))
        var table = assertInstanceOf(JvmPlv8Table::class.java, result)
        assertEquals(1, table.rows.size)
        assertTrue(XYZ_EXEC_RETAINED == table.rows[0][RET_OP] || XYZ_EXEC_DELETED == table.rows[0][RET_OP]) { table.rows[0][RET_ERR_MSG] }

        op = builder.buildXyzOp(XYZ_OP_CREATE, "v2_perf_test", null, "vgrid")
        feature = builder.buildFeatureFromMap(asMap(env.parse("""{"id":"v2_perf_test"}""")))
        result = session.writeCollections(arrayOf(op), arrayOf(feature), arrayOf(GEO_TYPE_NULL), arrayOf(null), arrayOf(null))
        table = assertInstanceOf(JvmPlv8Table::class.java, result)
        assertEquals(1, table.rows.size)
        assertTrue(XYZ_EXEC_CREATED == table.rows[0][RET_OP]) { table.rows[0][RET_ERR_MSG] }

        session.sql.execute("commit")

        val useBatch = false
        var totalTime = 0L
        var r = 0
        while (r++ < Rounds) {
            val features = createFeatures(FeaturesPerRound)
            val start = currentMicros()
            if (useBatch) {
                val stmt = conn.prepareStatement("INSERT INTO v2_perf_test (id, geo_grid, feature) VALUES (?, ?, ?)")
                stmt.use {
                    var i = 0
                    while (i < FeaturesPerRound) {
                        stmt.setString(1, features.idArr[i])
                        stmt.setString(2, "vgrid")
                        stmt.setBytes(3, features.featureArr[i])
                        stmt.addBatch()
                        i++
                    }
                    stmt.executeBatch()
                    conn.commit()
                }
            } else {
                val stmt = conn.prepareStatement("SELECT * FROM naksha_write_features(?, ?, ?, ?, ?, ?)")
                stmt.use {
                    stmt.setString(1, "v2_perf_test")
                    stmt.setArray(2, conn.createArrayOf(SQL_BYTE_ARRAY, features.opArr))
                    stmt.setArray(3, conn.createArrayOf(SQL_BYTE_ARRAY, features.featureArr))
                    stmt.setArray(4, conn.createArrayOf(SQL_INT16, features.geoTypeArr))
                    stmt.setArray(5, conn.createArrayOf(SQL_BYTE_ARRAY, features.geoArr))
                    stmt.setArray(6, conn.createArrayOf(SQL_BYTE_ARRAY, features.tagsArr))
                    stmt.executeQuery()
                    conn.commit()
                }
            }
            val end = currentMicros()
            totalTime += (end - start)
        }
        printStatistics(FeaturesPerRound, Rounds, totalTime, baseLine)
    }

    /**
     * Print statistics.
     * @param size Amount of features per round.
     * @param rounds Amount of rounds.
     * @param us Amount of microseconds the test took.
     * @param baseLine Base-line in microseconds.
     */
    private fun printStatistics(size: Int, rounds: Int, us: Long, baseLine: Double = 0.0): Double {
        val featuresWritten = (rounds * size).toDouble()
        val seconds = us.toDouble() / 1_000_000.0
        val featuresPerSecond = featuresWritten / seconds
        val microsPerFeature = us.toDouble() / featuresWritten
        if (baseLine == 0.0) {
            println("Write $featuresPerSecond features per second, ${microsPerFeature}us per feature, total time: ${seconds}s")
        } else {
            println("Write $featuresPerSecond features per second, ${microsPerFeature}us per feature (baseline=${baseLine}us), total time: ${seconds}s")
        }
        return microsPerFeature
    }

    data class BulkFeature(
            val id : String,
            val partId : Int,
            val op : ByteArray,
            val feature : ByteArray,
            val geoType : Short = GEO_TYPE_NULL,
            val geometry : ByteArray? = null,
            val tags : ByteArray? = null
    )

    private val xyzBuilder = XyzBuilder.create(65536)

    private fun createBulkFeature(id:String = env.randomString(12)) : BulkFeature {
        var topology = topologyTemplate
        if (topology == null) {
            topology = asMap(env.parse(topologyJson))
            topologyTemplate = topology
        }
        topology["id"] = id
        val partId = Static.partitionNumber(id)
        val op = xyzBuilder.buildXyzOp(XYZ_OP_CREATE, id, null, "vgrid")
        val feature = xyzBuilder.buildFeatureFromMap(topology)
        return BulkFeature(id, partId, op, feature)
    }

    @Order(3)
    @Test
    fun bulkLoadFeatures() {
        val session = NakshaSession.get()
        val builder = XyzBuilder.create(65536)
        val tableName = "v2_bulk_test"

        var op = builder.buildXyzOp(XYZ_OP_DELETE, "$tableName", null, "vgrid")
        var feature = builder.buildFeatureFromMap(asMap(env.parse("""{"id":"$tableName"}""")))
        var result = session.writeCollections(arrayOf(op), arrayOf(feature), arrayOf(GEO_TYPE_NULL), arrayOf(null), arrayOf(null))
        var table = assertInstanceOf(JvmPlv8Table::class.java, result)
        assertEquals(1, table.rows.size)
        assertTrue(XYZ_EXEC_RETAINED == table.rows[0][RET_OP] || XYZ_EXEC_DELETED == table.rows[0][RET_OP]) { table.rows[0][RET_ERR_MSG] }

        op = builder.buildXyzOp(XYZ_OP_CREATE, "$tableName", null, "vgrid")
        feature = builder.buildFeatureFromMap(asMap(env.parse("""{"id":"$tableName", "partition":true}""")))
        result = session.writeCollections(arrayOf(op), arrayOf(feature), arrayOf(GEO_TYPE_NULL), arrayOf(null), arrayOf(null))
        table = assertInstanceOf(JvmPlv8Table::class.java, result)
        assertEquals(1, table.rows.size)
        assertTrue(XYZ_EXEC_CREATED == table.rows[0][RET_OP]) { table.rows[0][RET_ERR_MSG] }

        session.sql.execute("commit")

        // Run for 8 threads.
        val features = Array<ArrayList<BulkFeature>>(BulkThreads) { ArrayList() }
        var i = 0
        while (i < BulkSize) {
            val f = createBulkFeature()
            val p = f.partId % BulkThreads
            val list = features[p]
            list.add(f)
            i++
        }
        i = 0
        while (i < features.size) {
            println("Features in bulk $i: ${features[i].size}")
            i++
        }
        val threads = Array(BulkThreads) { Thread {
            val threadId = it
            val list = features[it]
            val conn = DriverManager.getConnection(url)
            conn.use {
                val env = JvmPlv8Env.get()
                env.startSession(
                        conn,
                        schema,
                        "plv8_${threadId}_thread",
                        env.randomString(),
                        "plv8_${threadId}_app",
                        "plv8_${threadId}_author"
                )
                conn.commit()
                val stmt = conn.prepareStatement("INSERT INTO $tableName (id, geo_grid, feature) VALUES (?, ?, ?)")
                stmt.use {
                    var j = 0
                    while (j < list.size) {
                        val f = list[j++]
                        stmt.setString(1, f.id)
                        stmt.setString(2, "vgrid")
                        stmt.setBytes(3, f.feature)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                    conn.commit()
                }
            }
        } }
        val start = currentMicros()
        i = 0
        while (i < threads.size) {
            threads[i++].start()
        }
        i = 0
        while (i < threads.size) {
            threads[i++].join()
        }
        val end = currentMicros()
        printStatistics(BulkSize, 1, (end - start), baseLine)
    }
}