package naksha.model

import kotlin.test.Test
import kotlin.test.assertEquals

class XyzProxyTest {

//    @Test
//    fun testTagNormalization() {
//        // expect
//        assertEquals("zara", normalizeTag("žara"))
//        assertEquals("@žara", normalizeTag("@žara"))
//        assertEquals("", normalizeTag(""))
//        assertEquals("best", normalizeTag("BEST"))
//        assertEquals("#Best", normalizeTag("#Best"))
//        assertEquals("~Best", normalizeTag("~Best"))
//        assertEquals("ref_Best", normalizeTag("ref_Best"))
//        assertEquals("sourceID_Best", normalizeTag("sourceID_Best"))
//        assertEquals("+best", normalizeTag("+Best"))
//    }

//    @Test
//    fun testTagsNormalization() {
//        // given
//        val tags = TagList("A", "B", "#C")
//
//        // when
//        normalizeTags(tags)
//
//        // then
//        assertEquals(listOf("a", "b", "#C"), tags.toList())
//    }

    @Test
    fun testAddTag() {
        // given
        val xyz = XyzNs()

        /// when
        xyz.addTag("A", false)
        xyz.addTag("B", true)

        // then
        assertEquals(listOf("A", "b"), xyz.tags?.toList())
    }

    @Test
    fun testAddTags() {
        // given
        val xyz = XyzNs()

        /// when
        xyz.addTags(listOf("A", "B"), false)

        // then
        assertEquals(listOf("A", "B"), xyz.tags?.toList())
    }

    @Test
    fun testAddAndNormalizeTags() {
        // given
        val xyz = XyzNs()

        /// when
        xyz.addAndNormalizeTags("A", "B")

        // then
        assertEquals(listOf("a", "b"), xyz.tags?.toList())
    }

    @Test
    fun testRemoveTag() {
        // given
        val xyz = XyzNs()
        xyz.addTags(listOf("A", "B"), true)

        /// when
        xyz.removeTag("A", false)

        // then
        // not removed as it was not normalized
        assertEquals(listOf("a", "b"), xyz.tags?.toList())

        // when
        xyz.removeTag("A", true)

        // then
        assertEquals(listOf("b"), xyz.tags?.toList())
    }

    @Test
    fun testRemoveTags() {
        // given
        val xyz = XyzNs()
        xyz.addTags(listOf("A", "B"), true)

        /// when
        xyz.removeTags(listOf("A", "B"), false)

        // then
        // not removed as it was not normalized
        assertEquals(listOf("a", "b"), xyz.tags?.toList())

        // when
        xyz.removeTags(listOf("A", "B"), true)

        // then
        assertEquals(emptyList(), xyz.tags?.toList())
    }

    @Test
    fun testRemoveTagsWithPrefix() {
        // given
        val xyz = XyzNs()
        xyz.addTags(listOf("Alicja", "Baba", "Alan"), false)

        // when
        xyz.removeTagsWithPrefix("Al")

        // then
        assertEquals(listOf("Baba"), xyz.tags?.toList())
    }

    @Test
    fun testRemoveTagsWithPrefixNormalized() {
        // given
        val xyz = XyzNs()
        xyz.addTags(listOf("Alicja", "Baba", "Alan"), true)

        // when
        xyz.removeTagsWithPrefix("Al")

        // then
        // not removed because prefix is not normalized.
        assertEquals(listOf("alicja", "baba", "alan"), xyz.tags?.toList())
    }

    @Test
    fun testRemoveTagsWithPrefixes() {
        // given
        val xyz = XyzNs()
        xyz.addTags(listOf("Alicja", "Baba", "Alan"), false)

        // when
        xyz.removeTagsWithPrefixes(listOf("Al", "B"))

        // then
        assertEquals(emptyList(), xyz.tags?.toList())
    }

    @Test
    fun testSetTags() {
        // given
        val xyz = XyzNs()

        // when
        xyz.setTags(TagList("Alicja", "Baba", "Alan"), normalize = false)

        // then
        assertEquals(listOf("Alicja", "Baba", "Alan"), xyz.tags.toList())

        // when
        xyz.setTags(TagList("Cecil"), true)
        assertEquals(listOf("cecil"), xyz.tags.toList())
    }
}
