package naksha.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import naksha.base.Action
import naksha.base.NakshaError.NakshaErrorCompanion.UNINITIALIZED
import naksha.base.NakshaException
import naksha.base.TupleNumber
import naksha.base.Version
import naksha.model.objects.NakshaStorage
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class AbstractStorageTest {

    @Test
    fun newVirtualVersionUsesCurrentUtcDateAndIncrementsSequence() {
        val storage = TestStorage()
        while (true) {
            val before = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

            storage.resetNextVirtualVersion(Version.MIN_AUTO.number)
            val first = storage.newVersion()
            val second = storage.newVersion()

            val after = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            if (before != after) continue // This should only happen when the test is executed exactly around midnight UTC!

            assertEquals(before.year, first.year)
            assertEquals(before.month.number, first.month)
            assertEquals(before.day, first.day)
            assertEquals(Action.VERSION, Action.fromValue(first.number.toInt() and 3))
            assertEquals(first.number + 4, second.number)
            break
        }
    }

    @Test
    fun newVirtualVersionDoesNotRollBackSequenceWithNewerDate() {
        val previousDay = LocalDateTime(2026, 8, 24, 23, 59, 59)
        val nextDay = LocalDateTime(2026, 8, 25, 0, 0, 0)
        val previousDayVersion = Version.auto(2026, 8, 24, 0L, Action.VERSION)
        val nextDayVersion = Version.auto(2026, 8, 25, 0L, Action.VERSION)
        val storage = TestStorage()
        var competingVersion: Version? = null

        storage.resetNextVirtualVersion(previousDayVersion.number)
        storage.fakeVirtualVersionTime(
            listOf(nextDay, nextDay, previousDay),
            afterFirstAcquire = { competingVersion = storage.newVersion() },
        )
        val delayedVersion = storage.newVersion()

        assertNotNull(competingVersion)
        assertEquals(nextDayVersion.number, competingVersion.number)
        assertEquals(nextDayVersion.number + 4, delayedVersion.number)
        assertEquals(delayedVersion.number + 4, storage.peekNextVirtualVersion())
    }

    @Test
    fun newVirtualTupleNumberDerivesNumbersAndEncodesAction() {
        val storageId = "test-storage"
        val catalogId = "test-catalog"
        val collectionId = "test-collection"
        val featureId = "test-feature"
        val storage = TestStorage().apply { initialize(storageId) }
        val version = Version.auto(2026, 8, 24, 42L, Action.VERSION).number

        for (action in listOf(Action.CREATE, Action.UPDATE, Action.DELETE, Action.VERSION)) {
            val tupleNumber = storage.newTupleNumber(catalogId, collectionId, featureId, version, action)

            assertEquals(Naksha.databaseNumber(storageId), tupleNumber.databaseNumber)
            assertEquals(Naksha.catalogNumber(catalogId), tupleNumber.catalogNumber)
            assertEquals(Naksha.collectionNumber(collectionId), tupleNumber.collectionNumber)
            assertEquals(Naksha.featureNumber(featureId), tupleNumber.featureNumber)
            assertEquals(version.toLong() and -4L, tupleNumber.version.toLong() and -4L)
            assertEquals(action, tupleNumber.action)
        }
    }

    @Test
    fun newVirtualTupleNumberRequiresInitializedStorage() {
        val exception = assertFailsWith<NakshaException> {
            TestStorage().newTupleNumber("catalog", "collection", "feature", 3L, Action.VERSION)
        }

        assertEquals(UNINITIALIZED, exception.error.code)
    }

    @Test
    fun newVirtualVersion() {
        val storage = UnmodifiedTestStorage()
        val v1 = storage.newVersion()
        assertNotNull( v1 )
        assertTrue(v1.isDated(), "Expected a dated version, but got manual version")

        val v2 = storage.newVersion()
        assertNotNull( v2 )
        assertTrue(v2.isDated(), "Expected a dated version, but got manual version")

        assertNotEquals(v1, v2)
        assertEquals(v1.year, v2.year)
        assertEquals(v1.month, v2.month)
        assertEquals(v1.day, v2.day)
        assertEquals(v1.seq, v2.seq - 1)
    }

    private class UnmodifiedTestStorage : AbstractStorage<NakshaStorage>() {
        override val configKlass: KClass<NakshaStorage> = NakshaStorage::class
        fun newVersion(): Version = super.newVirtualVersion()
        override fun initStorage(config: NakshaStorage, create: Boolean?, upgrade: Boolean?) = Unit
        override fun afterInit() = Unit
        override fun shutdownStorage(dropCache: Boolean) = Unit
        override fun newWriteSession(options: SessionOptions?): IWriteSession = error("Not used by this test")
        override fun newReadSession(options: SessionOptions?): IReadSession = error("Not used by this test")
        override fun newStreamSession(options: SessionOptions?): IStreamSession = error("Not used by this test")
    }

    private class TestStorage : AbstractStorage<NakshaStorage>() {
        override val configKlass: KClass<NakshaStorage> = NakshaStorage::class
        private var fakeTimes: MutableList<LocalDateTime>? = null
        private var afterAcquire: (() -> Unit)? = null

        fun initialize(storageId: String) {
            invokeInitStorage(NakshaStorage(storageId, "TestStorage"), create = null, upgrade = null)
        }

        fun resetNextVirtualVersion(version: Long) {
            nextVirtualVersion.set(version)
        }

        fun peekNextVirtualVersion(): Long = nextVirtualVersion.get()

        fun fakeVirtualVersionTime(times: List<LocalDateTime>, afterFirstAcquire: () -> Unit) {
            fakeTimes = times.toMutableList()
            afterAcquire = afterFirstAcquire
        }

        fun newVersion(): Version = newVirtualVersion()

        override fun newVirtualVersion(): Version {
            val times = fakeTimes ?: return super.newVirtualVersion()
            while (true) {
                val version = Version(nextVirtualVersion.getAndAdd(4L))
                afterAcquire?.let { hook ->
                    afterAcquire = null
                    hook()
                }
                val now = times.removeAt(0)
                if (version.isBehind(now.year, now.month.number, now.day)) {
                    val newVersion = Version.auto(now.year, now.month.number, now.day, 0L, Action.VERSION)
                    if (nextVirtualVersion.compareAndSet(version.number + 4, newVersion.number + 4)) {
                        return newVersion
                    }
                    continue
                }
                return version
            }
        }

        fun newTupleNumber(
            catalogId: String,
            collectionId: String,
            featureId: String,
            version: Long,
            action: Action,
        ): TupleNumber = newVirtualTupleNumber(catalogId, collectionId, featureId, version, action)

        override fun initStorage(config: NakshaStorage, create: Boolean?, upgrade: Boolean?) = Unit
        override fun afterInit() = Unit
        override fun shutdownStorage(dropCache: Boolean) = Unit
        override fun newWriteSession(options: SessionOptions?): IWriteSession = error("Not used by this test")
        override fun newReadSession(options: SessionOptions?): IReadSession = error("Not used by this test")
        override fun newStreamSession(options: SessionOptions?): IStreamSession = error("Not used by this test")
    }
}
