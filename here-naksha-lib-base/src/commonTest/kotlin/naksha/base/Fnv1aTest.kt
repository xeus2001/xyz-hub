package naksha.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Fnv1aTest {
    @Test
    fun testFnv1a32() {
        // See: https://md5calc.com/hash/fnv1a32/
        val testString = "test"
        val expectedHash: Int = 0xafd071e5.toInt()
        var hash = Fnv1a32.start()
        var i = 0
        while (i < testString.length) {
            val c = testString[i++]
            hash = Fnv1a32.int8(hash, (c.code and 0xff).toByte())
        }
        assertEquals(expectedHash, hash)
    }

    @Test
    fun testFnv1a32Reverse() {
        // See: https://md5calc.com/hash/fnv1a32
        val testString = "test"
        val expectedHash: Int = 0xffcdb2a1.toInt()
        val hash = Fnv1a32.stringReverse(Fnv1a32.start(), testString)
        assertEquals(expectedHash, hash)
    }

    @Test
    fun testFnv1a64() {
        // See: https://toolkitbay.com/tkb/tool/FNV-1
        val testString = "test"
        val expectedHash = Int64(0xf9e6e6ef197c2b25uL.toLong())
        var hash = Fnv1a64.start()
        var i = 0
        while (i < testString.length) {
            val c = testString[i++]
            hash = Fnv1a64.int8(hash, (c.code and 0xff).toByte())
        }
        assertTrue(expectedHash eq hash)
        assertEquals(expectedHash, hash)
    }

    @Test
    fun testFnv1a32ByteArray() {
        // given
        val testString = "test"
        val testByteArray = testString.encodeToByteArray()

        // when
        val byteArrayHash = Fnv1a32.hashByteArray(testByteArray)
        val stringHash = Fnv1a32.string(Fnv1a32.start(), testString)

        // then
        assertEquals(byteArrayHash, stringHash)
    }

    @Test
    fun testFnv1a32NullByteArray() {
        // expect
        assertEquals(Fnv1a32.start(), Fnv1a32.hashByteArray(null))
    }
}